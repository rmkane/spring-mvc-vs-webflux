#!/usr/bin/env python3
"""
Bump Spring Boot and lombok.version in acme-dependencies; align maven-* plugin version properties in
acme-starter-parent with spring-boot-dependencies for that release (not inherited from BOM import).

lombok.version is taken from the same spring-boot-dependencies POM properties.

Which plugin properties to sync is discovered from starter-parent: any <properties> entry named
build-helper-maven-plugin.version or maven-*-plugin.version (e.g. maven-compiler-plugin.version).

Reads spring-boot-dependencies-${version}.pom from the local Maven repository.
The repo path is resolved like Maven does: MAVEN_OPTS -Dmaven.repo.local=..., then
<localRepository> in ~/.m2/settings.xml, else ~/.m2/repository.
If the POM is missing, runs mvn dependency:get (uses your settings.xml mirrors, etc.).

Usage:
  python3 update_spring_boot_version.py 3.6.2
  python3 update_spring_boot_version.py --dry-run 3.6.2
  python3 update_spring_boot_version.py -v 3.6.2
"""
from __future__ import annotations

import argparse
import logging
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path

# --- XML (schema constant; not part of job config) ---
POM_NS = {"p": "http://maven.apache.org/POM/4.0.0"}

# --- Managed plugin property names in starter-parent (regex; stable rule) ---
_MANAGED_PLUGIN_VERSION = re.compile(
    r"^(?:build-helper-maven-plugin|maven-.+-plugin)\.version$"
)


@dataclass(frozen=True)
class UpdateSpringBootConfig:
    """Tunable constants for one update run; construct in main (or tests) and thread through helpers."""

    spring_boot_bom_group_id: str
    spring_boot_bom_artifact_id: str
    spring_boot_version_property: str
    lombok_version_property: str
    acme_pom_aggregator_dir: str
    acme_dependencies_artifact_id: str
    acme_starter_parent_artifact_id: str
    rel_framework_pom: str
    m2_dir_name: str
    maven_settings_xml: str
    default_m2_repository_tail: tuple[str, str]
    maven_opts_env: str
    maven_repo_local_flag_prefix: str
    settings_xml_local_repository_tag_suffix: str
    mvn_executable: str
    mvn_flag_file: str
    mvn_dependency_plugin_coord: str
    mvn_goal_dependency_get: str
    mvn_flag_transitive_false: str
    mvn_flag_artifact_prefix: str

    @classmethod
    def defaults(cls) -> UpdateSpringBootConfig:
        return cls(
            spring_boot_bom_group_id="org.springframework.boot",
            spring_boot_bom_artifact_id="spring-boot-dependencies",
            spring_boot_version_property="spring-boot.version",
            lombok_version_property="lombok.version",
            acme_pom_aggregator_dir="acme-pom",
            acme_dependencies_artifact_id="acme-dependencies",
            acme_starter_parent_artifact_id="acme-starter-parent",
            rel_framework_pom="pom.xml",
            m2_dir_name=".m2",
            maven_settings_xml="settings.xml",
            default_m2_repository_tail=(".m2", "repository"),
            maven_opts_env="MAVEN_OPTS",
            maven_repo_local_flag_prefix="-Dmaven.repo.local=",
            settings_xml_local_repository_tag_suffix="localRepository",
            mvn_executable="mvn",
            mvn_flag_file="-f",
            mvn_dependency_plugin_coord="org.apache.maven.plugins:maven-dependency-plugin:3.10.0",
            mvn_goal_dependency_get="get",
            mvn_flag_transitive_false="-Dtransitive=false",
            mvn_flag_artifact_prefix="-Dartifact=",
        )

    @property
    def rel_framework_pom_path(self) -> Path:
        return Path(self.rel_framework_pom)

    @property
    def rel_acme_dependencies_pom(self) -> Path:
        return Path(
            self.acme_pom_aggregator_dir,
            self.acme_dependencies_artifact_id,
            self.rel_framework_pom,
        )

    @property
    def rel_acme_starter_parent_pom(self) -> Path:
        return Path(
            self.acme_pom_aggregator_dir,
            self.acme_starter_parent_artifact_id,
            self.rel_framework_pom,
        )

    @property
    def label_acme_dependencies_pom(self) -> str:
        """POSIX-style path for logs (Maven layout convention; not necessarily os.pathsep)."""
        return Path(self.acme_dependencies_artifact_id, self.rel_framework_pom).as_posix()

    @property
    def label_acme_starter_parent_pom(self) -> str:
        return Path(self.acme_starter_parent_artifact_id, self.rel_framework_pom).as_posix()


@dataclass(frozen=True)
class RunContext:
    cfg: UpdateSpringBootConfig
    log: logging.Logger


@dataclass(frozen=True)
class FrameworkLayout:
    """Absolute paths under acme-framework (see acme_framework_root)."""

    root: Path
    framework_pom: Path
    deps_pom: Path
    starter_pom: Path


@dataclass(frozen=True)
class PatchJob:
    path: Path
    label: str
    operations: tuple[tuple[str, str, str], ...]


@dataclass(frozen=True)
class VersionDriftReport:
    """Pure comparison of on-disk POM property maps vs target Boot line (no I/O)."""

    spring_property: str
    spring_on_disk: str
    spring_target: str
    lombok_property: str
    lombok_on_disk: str
    lombok_target: str
    bom_artifact_id: str
    label_deps_pom: str
    label_starter_pom: str
    plugin_mismatches: tuple[tuple[str, str, str], ...]  # prop, on_disk, bom_value
    matched_plugins: tuple[tuple[str, str], ...]  # prop, value (for DEBUG logs)


def compute_version_drift(
    *,
    cfg: UpdateSpringBootConfig,
    deps_props: dict[str, str],
    starter_props: dict[str, str],
    target_boot_version: str,
    target_lombok_version: str,
    plugin_keys: list[str],
    target_plugin_versions: dict[str, str],
) -> VersionDriftReport:
    cur_boot = deps_props.get(cfg.spring_boot_version_property, "<unset>")
    cur_lombok = deps_props.get(cfg.lombok_version_property, "<unset>")
    mismatches: list[tuple[str, str, str]] = []
    matched: list[tuple[str, str]] = []
    for key in plugin_keys:
        cur = starter_props.get(key, "<unset>")
        tgt = target_plugin_versions[key]
        if cur != tgt:
            mismatches.append((key, cur, tgt))
        else:
            matched.append((key, cur))
    return VersionDriftReport(
        spring_property=cfg.spring_boot_version_property,
        spring_on_disk=cur_boot,
        spring_target=target_boot_version,
        lombok_property=cfg.lombok_version_property,
        lombok_on_disk=cur_lombok,
        lombok_target=target_lombok_version,
        bom_artifact_id=cfg.spring_boot_bom_artifact_id,
        label_deps_pom=cfg.label_acme_dependencies_pom,
        label_starter_pom=cfg.label_acme_starter_parent_pom,
        plugin_mismatches=tuple(mismatches),
        matched_plugins=tuple(matched),
    )


@dataclass(frozen=True)
class PreparedUpdate:
    """Result of planning: drift report and file patches to apply."""

    drift: VersionDriftReport
    patch_jobs: tuple[PatchJob, ...]
    framework_pom: Path


# --- Pure helpers: Maven repo layout & coordinates ---


def fmt_pom_filename(artifact_id: str, version: str) -> str:
    return f"{artifact_id}-{version}.pom"


def fmt_maven_coord(
    group_id: str, artifact_id: str, version: str, *, packaging: str
) -> str:
    return f"{group_id}:{artifact_id}:{version}:{packaging}"


def maven_group_id_relative_path(group_id: str) -> Path:
    """Path segments under the local repo root for a Maven groupId (e.g. org.springframework.boot)."""
    return Path(*group_id.split("."))


def local_repo_artifact_version_dir(
    local_repo: Path, group_id: str, artifact_id: str, version: str
) -> Path:
    return local_repo / maven_group_id_relative_path(group_id) / artifact_id / version


def local_repo_pom_path(
    local_repo: Path, group_id: str, artifact_id: str, version: str
) -> Path:
    return local_repo_artifact_version_dir(local_repo, group_id, artifact_id, version) / fmt_pom_filename(
        artifact_id, version
    )


def fmt_mvn_plugin_prefix_goal(plugin_coord: str, goal: str) -> str:
    return f"{plugin_coord}:{goal}"


def mvn_dependency_get_argv(
    cfg: UpdateSpringBootConfig, framework_pom: Path, version: str
) -> list[str]:
    """argv for `mvn … dependency:get` to fetch the Boot BOM POM."""
    coord = fmt_maven_coord(
        cfg.spring_boot_bom_group_id,
        cfg.spring_boot_bom_artifact_id,
        version,
        packaging="pom",
    )
    return [
        cfg.mvn_executable,
        cfg.mvn_flag_file,
        str(framework_pom),
        fmt_mvn_plugin_prefix_goal(cfg.mvn_dependency_plugin_coord, cfg.mvn_goal_dependency_get),
        cfg.mvn_flag_transitive_false,
        f"{cfg.mvn_flag_artifact_prefix}{coord}",
    ]


def local_repo_from_maven_opts_string(prefix: str, maven_opts_value: str) -> Path | None:
    for token in maven_opts_value.split():
        if not token.startswith(prefix):
            continue
        raw = token.split("=", 1)[1].strip().strip('"').strip("'")
        return Path(raw).expanduser().resolve()
    return None


def local_repo_from_settings_root(
    settings_root: ET.Element,
    *,
    home: Path,
    m2_dir: str,
    local_repo_tag_suffix: str,
) -> Path | None:
    for el in settings_root.iter():
        if not el.tag.endswith(local_repo_tag_suffix):
            continue
        if not el.text or not el.text.strip():
            continue
        raw = el.text.strip()
        p = Path(raw)
        if p.is_absolute():
            return p.expanduser().resolve()
        return (home / Path(m2_dir) / raw).resolve()
    return None


def resolve_local_repository_path(
    cfg: UpdateSpringBootConfig,
    *,
    home: Path,
    maven_opts_value: str,
    settings_root: ET.Element | None,
) -> tuple[Path, str]:
    """Choose local repo path; return (path, source) where source is maven_opts|settings|default."""
    p = local_repo_from_maven_opts_string(cfg.maven_repo_local_flag_prefix, maven_opts_value)
    if p is not None:
        return p, "maven_opts"
    if settings_root is not None:
        p = local_repo_from_settings_root(
            settings_root,
            home=home,
            m2_dir=cfg.m2_dir_name,
            local_repo_tag_suffix=cfg.settings_xml_local_repository_tag_suffix,
        )
        if p is not None:
            return p, "settings"
    default = home.joinpath(*cfg.default_m2_repository_tail).resolve()
    return default, "default"


def xml_local_name(element: ET.Element) -> str:
    return element.tag.split("}", 1)[-1]


def acme_framework_root() -> Path:
    # This file lives at acme-framework/scripts/<name>.py; framework root is acme-framework/.
    return Path(__file__).resolve().parent.parent


def framework_layout(cfg: UpdateSpringBootConfig) -> FrameworkLayout:
    root = acme_framework_root()
    return FrameworkLayout(
        root=root,
        framework_pom=root / cfg.rel_framework_pom_path,
        deps_pom=root / cfg.rel_acme_dependencies_pom,
        starter_pom=root / cfg.rel_acme_starter_parent_pom,
    )


def parse_pom_properties_root(pom_path: Path) -> ET.Element:
    root = ET.parse(pom_path).getroot()
    props_el = root.find("p:properties", POM_NS)
    if props_el is None:
        raise ValueError(f"{pom_path}: no <properties>")
    return props_el


def pom_properties_to_map(props_el: ET.Element) -> dict[str, str]:
    out: dict[str, str] = {}
    for el in props_el:
        name = xml_local_name(el)
        if el.text and el.text.strip():
            out[name] = el.text.strip()
    return out


def load_pom_simple_properties(pom_path: Path) -> dict[str, str]:
    return pom_properties_to_map(parse_pom_properties_root(pom_path))


def discover_managed_plugin_property_names(starter_pom: Path) -> list[str]:
    props_el = parse_pom_properties_root(starter_pom)
    keys = [xml_local_name(el) for el in props_el if _MANAGED_PLUGIN_VERSION.match(xml_local_name(el))]
    keys.sort()
    if not keys:
        raise ValueError(
            f"{starter_pom}: no properties matching maven plugin version pattern "
            "(build-helper-maven-plugin.version or maven-*-plugin.version)"
        )
    return keys


def plugin_versions_for_keys(
    boot_props: dict[str, str], keys: list[str], bom_artifact_id: str
) -> dict[str, str]:
    missing = [k for k in keys if k not in boot_props]
    if missing:
        raise ValueError(
            f"{bom_artifact_id} POM missing properties "
            "(add to starter-parent only if Boot defines them): "
            + ", ".join(missing)
        )
    return {k: boot_props[k] for k in keys}


def replace_first_property_xml(
    text: str, prop_name: str, new_value: str, *, context: str
) -> tuple[str, str]:
    open_ = rf"<(?:[a-zA-Z0-9_.-]*:)?{re.escape(prop_name)}>"
    close = rf"</(?:[a-zA-Z0-9_.-]*:)?{re.escape(prop_name)}>"
    pattern = re.compile(rf"({open_})([^<]*)({close})")
    m = pattern.search(text)
    if not m:
        raise ValueError(f"{context}: could not find <{prop_name}> to update")
    old = m.group(2)
    replacement = m.group(1) + new_value + m.group(3)
    return text[: m.start()] + replacement + text[m.end() :], old


def apply_pom_property_operations(
    text: str,
    label: str,
    operations: Sequence[tuple[str, str, str]],
) -> tuple[str, tuple[tuple[str, str, str], ...]]:
    """
    Apply (prop_name, new_value, desc) in order. Pure.
    Returns (new_text, tuple of (desc, old, new) for props that changed).
    """
    changes: list[tuple[str, str, str]] = []
    cur = text
    for prop_name, new_val, desc in operations:
        cur, old_val = replace_first_property_xml(cur, prop_name, new_val, context=label)
        if old_val != new_val:
            changes.append((desc, old_val, new_val))
    return cur, tuple(changes)


# --- CLI / side effects ---


def configure_logging(*, verbose: bool, quiet: bool) -> None:
    if quiet and verbose:
        print("Cannot use both --verbose and --quiet", file=sys.stderr)
        sys.exit(1)
    if quiet:
        level = logging.WARNING
    elif verbose:
        level = logging.DEBUG
    else:
        level = logging.INFO
    logging.basicConfig(
        level=level,
        format="%(levelname)s: %(message)s",
        stream=sys.stderr,
        force=True,
    )


def load_settings_xml_root_if_present(cfg: UpdateSpringBootConfig, home: Path) -> ET.Element | None:
    path = home / Path(cfg.m2_dir_name, cfg.maven_settings_xml)
    if not path.is_file():
        return None
    return ET.parse(path).getroot()


def resolve_maven_local_repository(cfg: UpdateSpringBootConfig, log: logging.Logger) -> Path:
    home = Path.home()
    maven_opts = os.environ.get(cfg.maven_opts_env, "")
    settings_root = load_settings_xml_root_if_present(cfg, home)
    repo, source = resolve_local_repository_path(cfg, home=home, maven_opts_value=maven_opts, settings_root=settings_root)
    log.debug("Local repository (%s): %s", source, repo)
    return repo


def emit_version_drift_logs(report: VersionDriftReport, log: logging.Logger) -> None:
    log.info(
        "Checking drift vs target Boot (%s=%s, BOM %s)",
        report.spring_property,
        report.spring_target,
        report.bom_artifact_id,
    )
    if report.spring_on_disk != report.spring_target:
        log.warning(
            "[%s] %s differs: on disk %r, target %r",
            report.label_deps_pom,
            report.spring_property,
            report.spring_on_disk,
            report.spring_target,
        )
    else:
        log.info(
            "[%s] %s matches target (%s)",
            report.label_deps_pom,
            report.spring_property,
            report.spring_on_disk,
        )

    if report.lombok_on_disk != report.lombok_target:
        log.warning(
            "[%s] %s differs: on disk %r, %s has %r",
            report.label_deps_pom,
            report.lombok_property,
            report.lombok_on_disk,
            report.bom_artifact_id,
            report.lombok_target,
        )
    else:
        log.info(
            "[%s] %s matches %s (%s)",
            report.label_deps_pom,
            report.lombok_property,
            report.bom_artifact_id,
            report.lombok_on_disk,
        )

    for prop, cur, tgt in report.plugin_mismatches:
        log.warning(
            "[%s] %s differs: on disk %r, %s has %r",
            report.label_starter_pom,
            prop,
            cur,
            report.bom_artifact_id,
            tgt,
        )
    for prop, val in report.matched_plugins:
        log.debug("[%s] %s OK (%s)", report.label_starter_pom, prop, val)

    n_mis = len(report.plugin_mismatches)
    n_keys = n_mis + len(report.matched_plugins)
    spring_ok = report.spring_on_disk == report.spring_target
    lombok_ok = report.lombok_on_disk == report.lombok_target
    deps_pom_ok = spring_ok and lombok_ok

    if n_mis == 0 and deps_pom_ok:
        log.info(
            "No drift: %s, %s and all %d managed plugin pins already match Boot %s",
            report.spring_property,
            report.lombok_property,
            n_keys,
            report.spring_target,
        )
    elif n_mis:
        log.info(
            "Plugin pin drift: %d of %d properties differ from %s for Boot %s",
            n_mis,
            n_keys,
            report.bom_artifact_id,
            report.spring_target,
        )
    elif not deps_pom_ok:
        drift_names = []
        if not spring_ok:
            drift_names.append(report.spring_property)
        if not lombok_ok:
            drift_names.append(report.lombok_property)
        log.info(
            "Only %s drift; all %d managed plugin pins already match %s for Boot %s",
            " and ".join(drift_names),
            n_keys,
            report.bom_artifact_id,
            report.spring_target,
        )


# --- Plan (prepare) vs execute ---


def local_boot_dependencies_pom_path(
    cfg: UpdateSpringBootConfig, log: logging.Logger, version: str
) -> Path:
    """Expected path to spring-boot-dependencies-${version}.pom in the local Maven repo."""
    return local_repo_pom_path(
        resolve_maven_local_repository(cfg, log),
        cfg.spring_boot_bom_group_id,
        cfg.spring_boot_bom_artifact_id,
        version,
    )


def fetch_boot_dependencies_pom_if_missing(
    ctx: RunContext,
    framework_pom: Path,
    bom_pom_path: Path,
    version: str,
) -> None:
    """If the BOM POM is absent, run mvn dependency:get. Raises RuntimeError on failure."""
    cfg = ctx.cfg
    log = ctx.log
    if bom_pom_path.is_file():
        return
    log.info(
        "Boot BOM POM not in local repo; running mvn dependency:get for %s:%s ...",
        cfg.spring_boot_bom_artifact_id,
        version,
    )
    proc = subprocess.run(mvn_dependency_get_argv(cfg, framework_pom, version), check=False)
    if proc.returncode != 0:
        raise RuntimeError("mvn dependency:get failed")
    if not bom_pom_path.is_file():
        raise RuntimeError(f"Expected POM at {bom_pom_path} after dependency:get")


def prepare_update(
    ctx: RunContext, layout: FrameworkLayout, target_version: str
) -> PreparedUpdate:
    """
    Discover plugin keys, ensure Boot BOM POM is available, load properties, compute drift,
    and build patch jobs. Raises ValueError for invalid inputs / missing BOM properties;
    RuntimeError if dependency:get fails.
    """
    cfg = ctx.cfg
    log = ctx.log
    plugin_keys = discover_managed_plugin_property_names(layout.starter_pom)
    bom_pom = local_boot_dependencies_pom_path(cfg, log, target_version)
    fetch_boot_dependencies_pom_if_missing(ctx, layout.framework_pom, bom_pom, target_version)
    log.info("Using Boot BOM POM: %s", bom_pom)

    boot_props = load_pom_simple_properties(bom_pom)
    if cfg.lombok_version_property not in boot_props:
        raise ValueError(
            f"{cfg.spring_boot_bom_artifact_id} POM missing property "
            f"{cfg.lombok_version_property!r}"
        )
    target_lombok = boot_props[cfg.lombok_version_property]
    versions = plugin_versions_for_keys(
        boot_props, plugin_keys, cfg.spring_boot_bom_artifact_id
    )

    deps_props = load_pom_simple_properties(layout.deps_pom)
    starter_props = load_pom_simple_properties(layout.starter_pom)
    drift = compute_version_drift(
        cfg=cfg,
        deps_props=deps_props,
        starter_props=starter_props,
        target_boot_version=target_version,
        target_lombok_version=target_lombok,
        plugin_keys=plugin_keys,
        target_plugin_versions=versions,
    )

    patch_jobs = (
        PatchJob(
            path=layout.deps_pom,
            label=cfg.label_acme_dependencies_pom,
            operations=(
                (cfg.spring_boot_version_property, target_version, cfg.spring_boot_version_property),
                (cfg.lombok_version_property, target_lombok, cfg.lombok_version_property),
            ),
        ),
        PatchJob(
            path=layout.starter_pom,
            label=cfg.label_acme_starter_parent_pom,
            operations=tuple((k, versions[k], k) for k in plugin_keys),
        ),
    )
    return PreparedUpdate(drift=drift, patch_jobs=patch_jobs, framework_pom=layout.framework_pom)


def execute_prepared_update(ctx: RunContext, prepared: PreparedUpdate, *, dry_run: bool) -> None:
    """Log drift, apply patch jobs, print post-run Maven hint."""
    cfg = ctx.cfg
    log = ctx.log
    emit_version_drift_logs(prepared.drift, log)
    for job in prepared.patch_jobs:
        patch_file_from_operations(job.path, job.label, job.operations, dry_run, log)
    log.info(
        'Done. Run: %s %s "%s" -q -DskipTests validate',
        cfg.mvn_executable,
        cfg.mvn_flag_file,
        prepared.framework_pom,
    )


def patch_file_from_operations(
    path: Path,
    label: str,
    operations: Sequence[tuple[str, str, str]],
    dry_run: bool,
    log: logging.Logger,
) -> None:
    if not operations:
        return

    original = path.read_text(encoding="utf-8")
    text, changes = apply_pom_property_operations(original, label, operations)

    for desc, old_val, new_val in changes:
        log.info("[%s] updating %s: %r -> %r", label, desc, old_val, new_val)

    if text == original:
        log.info("[%s] no file changes needed", label)
        return
    if dry_run:
        log.warning("[%s] dry-run: not writing file", label)
        return
    path.write_text(text, encoding="utf-8", newline="\n")
    log.info("[%s] wrote %s", label, path)


def run(ctx: RunContext, target_version: str, *, dry_run: bool) -> None:
    layout = framework_layout(ctx.cfg)
    prepared = prepare_update(ctx, layout, target_version)
    execute_prepared_update(ctx, prepared, dry_run=dry_run)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Update Spring Boot and Lombok in acme-dependencies; sync Maven plugin versions in starter-parent."
    )
    parser.add_argument("--dry-run", action="store_true", help="Print changes only; do not write POMs")
    parser.add_argument("-v", "--verbose", action="store_true", help="Log DEBUG (per-property OK lines, paths)")
    parser.add_argument("-q", "--quiet", action="store_true", help="Only WARNING and above")
    parser.add_argument("version", help="spring-boot.version (e.g. 3.6.2)")
    args = parser.parse_args()

    configure_logging(verbose=args.verbose, quiet=args.quiet)

    log = logging.getLogger(__name__)
    cfg = UpdateSpringBootConfig.defaults()
    try:
        run(RunContext(cfg=cfg, log=log), args.version, dry_run=args.dry_run)
    except (ValueError, RuntimeError) as e:
        log.error("%s", e)
        sys.exit(1)


if __name__ == "__main__":
    main()
