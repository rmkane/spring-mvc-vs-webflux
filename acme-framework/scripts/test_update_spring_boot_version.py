#!/usr/bin/env python3
"""
Unit tests for update_spring_boot_version.py.

Covers pure helpers exhaustively and I/O helpers via tmp_path / monkeypatching;
no network or Maven installation required.

Run:
  python3 -m pytest test_update_spring_boot_version.py -v
  # or plain unittest:
  python3 -m unittest test_update_spring_boot_version -v
"""
from __future__ import annotations

import logging
import textwrap
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import MagicMock, patch
import tempfile

# ---------------------------------------------------------------------------
# Bootstrap: make the module importable from the same directory or scripts/.
# ---------------------------------------------------------------------------
import importlib
import sys

_MOD_NAME = "update_spring_boot_version"
try:
    mod = importlib.import_module(_MOD_NAME)
except ModuleNotFoundError:
    # Try sibling directory "scripts/"
    sys.path.insert(0, str(Path(__file__).resolve().parent / "scripts"))
    mod = importlib.import_module(_MOD_NAME)

(
    UpdateSpringBootConfig,
    RunContext,
    FrameworkLayout,
    PatchJob,
    VersionDriftReport,
    PreparedUpdate,
    # pure helpers
    fmt_pom_filename,
    fmt_maven_coord,
    maven_group_id_relative_path,
    local_repo_artifact_version_dir,
    local_repo_pom_path,
    fmt_mvn_plugin_prefix_goal,
    mvn_dependency_get_argv,
    local_repo_from_maven_opts_string,
    local_repo_from_settings_root,
    resolve_local_repository_path,
    xml_local_name,
    pom_properties_to_map,
    load_pom_simple_properties,
    discover_managed_plugin_property_names,
    plugin_versions_for_keys,
    replace_first_property_xml,
    apply_pom_property_operations,
    compute_version_drift,
    # I/O helpers
    resolve_maven_local_repository,
    load_settings_xml_root_if_present,
    local_boot_dependencies_pom_path,
    fetch_boot_dependencies_pom_if_missing,
    patch_file_from_operations,
    framework_layout,
    prepare_update,
    execute_prepared_update,
    run,
) = (
    getattr(mod, n)
    for n in [
        "UpdateSpringBootConfig",
        "RunContext",
        "FrameworkLayout",
        "PatchJob",
        "VersionDriftReport",
        "PreparedUpdate",
        "fmt_pom_filename",
        "fmt_maven_coord",
        "maven_group_id_relative_path",
        "local_repo_artifact_version_dir",
        "local_repo_pom_path",
        "fmt_mvn_plugin_prefix_goal",
        "mvn_dependency_get_argv",
        "local_repo_from_maven_opts_string",
        "local_repo_from_settings_root",
        "resolve_local_repository_path",
        "xml_local_name",
        "pom_properties_to_map",
        "load_pom_simple_properties",
        "discover_managed_plugin_property_names",
        "plugin_versions_for_keys",
        "replace_first_property_xml",
        "apply_pom_property_operations",
        "compute_version_drift",
        "resolve_maven_local_repository",
        "load_settings_xml_root_if_present",
        "local_boot_dependencies_pom_path",
        "fetch_boot_dependencies_pom_if_missing",
        "patch_file_from_operations",
        "framework_layout",
        "prepare_update",
        "execute_prepared_update",
        "run",
    ]
)

CFG = UpdateSpringBootConfig.defaults()
LOG = logging.getLogger("test")


def make_ctx(cfg: UpdateSpringBootConfig = CFG) -> RunContext:
    return RunContext(cfg=cfg, log=LOG)


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

DEPS_POM_TEMPLATE = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <properties>
    <spring-boot.version>{spring_boot}</spring-boot.version>
    <lombok.version>{lombok}</lombok.version>
  </properties>
</project>
"""

STARTER_POM_TEMPLATE = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <properties>
    <maven-compiler-plugin.version>{compiler}</maven-compiler-plugin.version>
    <maven-surefire-plugin.version>{surefire}</maven-surefire-plugin.version>
    <build-helper-maven-plugin.version>{build_helper}</build-helper-maven-plugin.version>
  </properties>
</project>
"""

BOOT_BOM_TEMPLATE = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <properties>
    <lombok.version>{lombok}</lombok.version>
    <maven-compiler-plugin.version>{compiler}</maven-compiler-plugin.version>
    <maven-surefire-plugin.version>{surefire}</maven-surefire-plugin.version>
    <build-helper-maven-plugin.version>{build_helper}</build-helper-maven-plugin.version>
  </properties>
</project>
"""

SETTINGS_XML_TEMPLATE = """\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <localRepository>{local_repo}</localRepository>
</settings>
"""


def write_pom(path: Path, content: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return path


# ===========================================================================
# 1. Pure helpers: Maven coordinate / path layout
# ===========================================================================

class TestFmtPomFilename(unittest.TestCase):
    def test_basic(self):
        self.assertEqual(
            fmt_pom_filename("spring-boot-dependencies", "3.4.0"),
            "spring-boot-dependencies-3.4.0.pom",
        )


class TestFmtMavenCoord(unittest.TestCase):
    def test_pom_packaging(self):
        coord = fmt_maven_coord("org.springframework.boot", "spring-boot-dependencies", "3.4.0", packaging="pom")
        self.assertEqual(coord, "org.springframework.boot:spring-boot-dependencies:3.4.0:pom")

    def test_jar_packaging(self):
        coord = fmt_maven_coord("com.example", "my-lib", "1.0.0", packaging="jar")
        self.assertEqual(coord, "com.example:my-lib:1.0.0:jar")


class TestMavenGroupIdRelativePath(unittest.TestCase):
    def test_three_segments(self):
        p = maven_group_id_relative_path("org.springframework.boot")
        self.assertEqual(p, Path("org") / "springframework" / "boot")

    def test_single_segment(self):
        self.assertEqual(maven_group_id_relative_path("acme"), Path("acme"))


class TestLocalRepoPomPath(unittest.TestCase):
    def test_full_path(self):
        repo = Path("/home/user/.m2/repository")
        result = local_repo_pom_path(
            repo, "org.springframework.boot", "spring-boot-dependencies", "3.4.0"
        )
        expected = (
            repo
            / "org" / "springframework" / "boot"
            / "spring-boot-dependencies"
            / "3.4.0"
            / "spring-boot-dependencies-3.4.0.pom"
        )
        self.assertEqual(result, expected)


class TestFmtMvnPluginPrefixGoal(unittest.TestCase):
    def test_format(self):
        result = fmt_mvn_plugin_prefix_goal(
            "org.apache.maven.plugins:maven-dependency-plugin:3.10.0", "get"
        )
        self.assertEqual(
            result,
            "org.apache.maven.plugins:maven-dependency-plugin:3.10.0:get",
        )


class TestMvnDependencyGetArgv(unittest.TestCase):
    def test_argv_structure(self):
        framework_pom = Path("/repo/pom.xml")
        argv = mvn_dependency_get_argv(CFG, framework_pom, "3.4.0")
        self.assertEqual(argv[0], "mvn")
        self.assertIn("-f", argv)
        self.assertIn(str(framework_pom), argv)
        artifact_arg = next(a for a in argv if a.startswith("-Dartifact="))
        self.assertIn("org.springframework.boot:spring-boot-dependencies:3.4.0:pom", artifact_arg)
        self.assertIn("-Dtransitive=false", argv)


# ===========================================================================
# 2. Pure helpers: Maven repo path resolution
# ===========================================================================

class TestLocalRepoFromMavenOptsString(unittest.TestCase):
    def test_finds_flag(self):
        result = local_repo_from_maven_opts_string(
            "-Dmaven.repo.local=",
            "-Xmx512m -Dmaven.repo.local=/custom/repo -Dfoo=bar",
        )
        self.assertEqual(result, Path("/custom/repo").resolve())

    def test_quoted_path(self):
        result = local_repo_from_maven_opts_string(
            "-Dmaven.repo.local=",
            '-Dmaven.repo.local="/custom/repo with spaces"',
        )
        self.assertIsNotNone(result)

    def test_missing_flag_returns_none(self):
        result = local_repo_from_maven_opts_string("-Dmaven.repo.local=", "-Xmx512m")
        self.assertIsNone(result)

    def test_empty_string_returns_none(self):
        self.assertIsNone(local_repo_from_maven_opts_string("-Dmaven.repo.local=", ""))


class TestLocalRepoFromSettingsRoot(unittest.TestCase):
    def _parse(self, xml_text: str) -> ET.Element:
        return ET.fromstring(xml_text)

    def test_absolute_path(self):
        root = self._parse(SETTINGS_XML_TEMPLATE.format(local_repo="/opt/maven/repo"))
        result = local_repo_from_settings_root(
            root,
            home=Path("/home/user"),
            m2_dir=".m2",
            local_repo_tag_suffix="localRepository",
        )
        self.assertEqual(result, Path("/opt/maven/repo"))

    def test_no_local_repository_tag(self):
        root = self._parse("<settings/>")
        result = local_repo_from_settings_root(
            root,
            home=Path("/home/user"),
            m2_dir=".m2",
            local_repo_tag_suffix="localRepository",
        )
        self.assertIsNone(result)

    def test_empty_tag_returns_none(self):
        root = self._parse("<settings><localRepository>   </localRepository></settings>")
        result = local_repo_from_settings_root(
            root,
            home=Path("/home/user"),
            m2_dir=".m2",
            local_repo_tag_suffix="localRepository",
        )
        self.assertIsNone(result)


class TestResolveLocalRepositoryPath(unittest.TestCase):
    def test_maven_opts_wins(self):
        path, source = resolve_local_repository_path(
            CFG,
            home=Path("/home/user"),
            maven_opts_value="-Dmaven.repo.local=/opts/repo",
            settings_root=None,
        )
        self.assertEqual(source, "maven_opts")
        self.assertEqual(path, Path("/opts/repo").resolve())

    def test_settings_used_when_no_opts(self):
        settings_root = ET.fromstring(
            SETTINGS_XML_TEMPLATE.format(local_repo="/settings/repo")
        )
        path, source = resolve_local_repository_path(
            CFG,
            home=Path("/home/user"),
            maven_opts_value="",
            settings_root=settings_root,
        )
        self.assertEqual(source, "settings")
        self.assertEqual(path, Path("/settings/repo"))

    def test_default_fallback(self):
        home = Path("/home/user")
        path, source = resolve_local_repository_path(
            CFG,
            home=home,
            maven_opts_value="",
            settings_root=None,
        )
        self.assertEqual(source, "default")
        self.assertEqual(path, (home / ".m2" / "repository").resolve())


# ===========================================================================
# 3. Pure helpers: XML / POM parsing
# ===========================================================================

class TestXmlLocalName(unittest.TestCase):
    def test_namespaced(self):
        el = ET.fromstring("<p:foo xmlns:p='urn:test'>bar</p:foo>")
        self.assertEqual(xml_local_name(el), "foo")

    def test_unnamespaced(self):
        el = ET.fromstring("<foo>bar</foo>")
        self.assertEqual(xml_local_name(el), "foo")


class TestPomPropertiesToMap(unittest.TestCase):
    def _props_el(self, xml_text: str) -> ET.Element:
        return ET.fromstring(xml_text)

    def test_basic_properties(self):
        el = self._props_el(
            "<properties>"
            "<spring-boot.version>3.4.0</spring-boot.version>"
            "<lombok.version>1.18.34</lombok.version>"
            "</properties>"
        )
        result = pom_properties_to_map(el)
        self.assertEqual(result["spring-boot.version"], "3.4.0")
        self.assertEqual(result["lombok.version"], "1.18.34")

    def test_empty_tag_excluded(self):
        el = self._props_el(
            "<properties><empty-prop></empty-prop><real-prop>1.0</real-prop></properties>"
        )
        result = pom_properties_to_map(el)
        self.assertNotIn("empty-prop", result)
        self.assertIn("real-prop", result)

    def test_whitespace_only_excluded(self):
        el = self._props_el(
            "<properties><whitespace-prop>   </whitespace-prop></properties>"
        )
        result = pom_properties_to_map(el)
        self.assertNotIn("whitespace-prop", result)

    def test_values_are_stripped(self):
        el = self._props_el(
            "<properties><ver>  3.4.0  </ver></properties>"
        )
        self.assertEqual(pom_properties_to_map(el)["ver"], "3.4.0")


class TestLoadPomSimpleProperties(unittest.TestCase):
    def test_loads_from_file(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            write_pom(
                pom,
                DEPS_POM_TEMPLATE.format(spring_boot="3.4.0", lombok="1.18.34"),
            )
            props = load_pom_simple_properties(pom)
        self.assertEqual(props["spring-boot.version"], "3.4.0")
        self.assertEqual(props["lombok.version"], "1.18.34")

    def test_raises_on_missing_properties_element(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            pom.write_text(
                '<?xml version="1.0"?>'
                '<project xmlns="http://maven.apache.org/POM/4.0.0"></project>',
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                load_pom_simple_properties(pom)


class TestDiscoverManagedPluginPropertyNames(unittest.TestCase):
    def test_discovers_all_matching_keys_sorted(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            write_pom(
                pom,
                STARTER_POM_TEMPLATE.format(
                    compiler="3.13.0", surefire="3.2.5", build_helper="3.6.0"
                ),
            )
            keys = discover_managed_plugin_property_names(pom)
        self.assertEqual(
            keys,
            [
                "build-helper-maven-plugin.version",
                "maven-compiler-plugin.version",
                "maven-surefire-plugin.version",
            ],
        )

    def test_raises_when_no_matching_keys(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            write_pom(
                pom,
                '<?xml version="1.0"?>'
                '<project xmlns="http://maven.apache.org/POM/4.0.0">'
                "<properties><unrelated.version>1.0</unrelated.version></properties>"
                "</project>",
            )
            with self.assertRaises(ValueError, msg="should raise on no matching keys"):
                discover_managed_plugin_property_names(pom)


class TestPluginVersionsForKeys(unittest.TestCase):
    def test_returns_matched_subset(self):
        boot_props = {
            "maven-compiler-plugin.version": "3.13.0",
            "maven-surefire-plugin.version": "3.2.5",
            "unrelated": "x",
        }
        result = plugin_versions_for_keys(
            boot_props,
            ["maven-compiler-plugin.version", "maven-surefire-plugin.version"],
            "spring-boot-dependencies",
        )
        self.assertEqual(result["maven-compiler-plugin.version"], "3.13.0")
        self.assertEqual(result["maven-surefire-plugin.version"], "3.2.5")
        self.assertNotIn("unrelated", result)

    def test_raises_on_missing_key(self):
        with self.assertRaises(ValueError) as ctx:
            plugin_versions_for_keys({}, ["missing-plugin.version"], "spring-boot-dependencies")
        self.assertIn("missing-plugin.version", str(ctx.exception))


# ===========================================================================
# 4. Pure helpers: text patching
# ===========================================================================

class TestReplaceFirstPropertyXml(unittest.TestCase):
    def _pom(self, version: str) -> str:
        return (
            "<properties>\n"
            f"  <spring-boot.version>{version}</spring-boot.version>\n"
            "</properties>"
        )

    def test_replaces_value(self):
        text, old = replace_first_property_xml(
            self._pom("3.3.0"), "spring-boot.version", "3.4.0", context="test"
        )
        self.assertIn("<spring-boot.version>3.4.0</spring-boot.version>", text)
        self.assertEqual(old, "3.3.0")

    def test_returns_old_value(self):
        _, old = replace_first_property_xml(
            self._pom("3.3.0"), "spring-boot.version", "3.4.0", context="test"
        )
        self.assertEqual(old, "3.3.0")

    def test_raises_when_property_not_found(self):
        with self.assertRaises(ValueError):
            replace_first_property_xml(
                "<properties/>", "nonexistent.version", "1.0", context="test"
            )

    def test_idempotent_when_value_unchanged(self):
        original = self._pom("3.4.0")
        text, old = replace_first_property_xml(
            original, "spring-boot.version", "3.4.0", context="test"
        )
        self.assertEqual(text, original)
        self.assertEqual(old, "3.4.0")

    def test_hyphenated_property_name(self):
        xml = "<properties><my-prop.version>1.0</my-prop.version></properties>"
        text, old = replace_first_property_xml(xml, "my-prop.version", "2.0", context="t")
        self.assertIn("2.0", text)
        self.assertEqual(old, "1.0")

    def test_namespaced_tag(self):
        xml = (
            '<properties xmlns:p="urn:x">'
            "<p:some-prop>old</p:some-prop>"
            "</properties>"
        )
        text, old = replace_first_property_xml(xml, "some-prop", "new", context="t")
        self.assertEqual(old, "old")
        self.assertIn("new", text)


class TestApplyPomPropertyOperations(unittest.TestCase):
    def _pom(self) -> str:
        return textwrap.dedent("""\
            <properties>
              <spring-boot.version>3.3.0</spring-boot.version>
              <lombok.version>1.18.30</lombok.version>
            </properties>
        """)

    def test_applies_multiple_operations(self):
        ops = [
            ("spring-boot.version", "3.4.0", "spring-boot.version"),
            ("lombok.version", "1.18.34", "lombok.version"),
        ]
        text, changes = apply_pom_property_operations(self._pom(), "label", ops)
        self.assertIn("3.4.0", text)
        self.assertIn("1.18.34", text)
        self.assertEqual(len(changes), 2)

    def test_no_change_when_values_match(self):
        ops = [("spring-boot.version", "3.3.0", "spring-boot.version")]
        _, changes = apply_pom_property_operations(self._pom(), "label", ops)
        self.assertEqual(len(changes), 0)

    def test_changes_tuple_contains_desc_old_new(self):
        ops = [("spring-boot.version", "3.4.0", "my-desc")]
        _, changes = apply_pom_property_operations(self._pom(), "label", ops)
        self.assertEqual(len(changes), 1)
        desc, old, new = changes[0]
        self.assertEqual(desc, "my-desc")
        self.assertEqual(old, "3.3.0")
        self.assertEqual(new, "3.4.0")

    def test_empty_operations_returns_unchanged(self):
        original = self._pom()
        text, changes = apply_pom_property_operations(original, "label", [])
        self.assertEqual(text, original)
        self.assertEqual(changes, ())


# ===========================================================================
# 5. compute_version_drift
# ===========================================================================

class TestComputeVersionDrift(unittest.TestCase):
    _PLUGIN_KEYS = ["maven-compiler-plugin.version", "maven-surefire-plugin.version"]

    def _drift(
        self,
        *,
        boot_on_disk="3.3.0",
        lombok_on_disk="1.18.30",
        compiler_on_disk="3.11.0",
        surefire_on_disk="3.1.2",
        target_boot="3.4.0",
        target_lombok="1.18.34",
        target_compiler="3.13.0",
        target_surefire="3.2.5",
    ) -> VersionDriftReport:
        return compute_version_drift(
            cfg=CFG,
            deps_props={
                "spring-boot.version": boot_on_disk,
                "lombok.version": lombok_on_disk,
            },
            starter_props={
                "maven-compiler-plugin.version": compiler_on_disk,
                "maven-surefire-plugin.version": surefire_on_disk,
            },
            target_boot_version=target_boot,
            target_lombok_version=target_lombok,
            plugin_keys=self._PLUGIN_KEYS,
            target_plugin_versions={
                "maven-compiler-plugin.version": target_compiler,
                "maven-surefire-plugin.version": target_surefire,
            },
        )

    def test_all_differ(self):
        report = self._drift()
        self.assertEqual(report.spring_on_disk, "3.3.0")
        self.assertEqual(report.spring_target, "3.4.0")
        self.assertEqual(report.lombok_on_disk, "1.18.30")
        self.assertEqual(report.lombok_target, "1.18.34")
        self.assertEqual(len(report.plugin_mismatches), 2)
        self.assertEqual(len(report.matched_plugins), 0)

    def test_all_match(self):
        report = self._drift(
            boot_on_disk="3.4.0",
            lombok_on_disk="1.18.34",
            compiler_on_disk="3.13.0",
            surefire_on_disk="3.2.5",
        )
        self.assertEqual(len(report.plugin_mismatches), 0)
        self.assertEqual(len(report.matched_plugins), 2)

    def test_partial_plugin_mismatch(self):
        report = self._drift(compiler_on_disk="3.13.0")  # compiler matches, surefire doesn't
        props = [m[0] for m in report.plugin_mismatches]
        self.assertNotIn("maven-compiler-plugin.version", props)
        self.assertIn("maven-surefire-plugin.version", props)

    def test_unset_fallback(self):
        report = compute_version_drift(
            cfg=CFG,
            deps_props={},  # nothing set
            starter_props={},
            target_boot_version="3.4.0",
            target_lombok_version="1.18.34",
            plugin_keys=self._PLUGIN_KEYS,
            target_plugin_versions={
                "maven-compiler-plugin.version": "3.13.0",
                "maven-surefire-plugin.version": "3.2.5",
            },
        )
        self.assertEqual(report.spring_on_disk, "<unset>")
        self.assertEqual(report.lombok_on_disk, "<unset>")

    def test_mismatch_tuple_contents(self):
        report = self._drift()
        mis = {m[0]: m for m in report.plugin_mismatches}
        prop, on_disk, target = mis["maven-compiler-plugin.version"]
        self.assertEqual(on_disk, "3.11.0")
        self.assertEqual(target, "3.13.0")


# ===========================================================================
# 6. I/O helpers: settings.xml loading
# ===========================================================================

class TestLoadSettingsXmlRootIfPresent(unittest.TestCase):
    def test_returns_root_when_file_exists(self):
        with tempfile.TemporaryDirectory() as td:
            home = Path(td)
            m2 = home / ".m2"
            m2.mkdir()
            (m2 / "settings.xml").write_text(
                SETTINGS_XML_TEMPLATE.format(local_repo="/custom/repo"), encoding="utf-8"
            )
            root = load_settings_xml_root_if_present(CFG, home)
        self.assertIsNotNone(root)

    def test_returns_none_when_file_absent(self):
        with tempfile.TemporaryDirectory() as td:
            root = load_settings_xml_root_if_present(CFG, Path(td))
        self.assertIsNone(root)


# ===========================================================================
# 7. fetch_boot_dependencies_pom_if_missing
# ===========================================================================

class TestFetchBootDependenciesPomIfMissing(unittest.TestCase):
    def test_no_op_when_file_exists(self):
        """Should not call subprocess when BOM POM already present."""
        with tempfile.TemporaryDirectory() as td:
            bom = Path(td) / "bom.pom"
            bom.write_text("<project/>", encoding="utf-8")
            ctx = make_ctx()
            # If subprocess.run were called this would blow up without mocking
            with patch.object(mod, "subprocess") as mock_sub:
                fetch_boot_dependencies_pom_if_missing(ctx, Path(td) / "pom.xml", bom, "3.4.0")
                mock_sub.run.assert_not_called()

    def test_raises_runtime_error_when_mvn_fails(self):
        with tempfile.TemporaryDirectory() as td:
            bom = Path(td) / "bom.pom"  # does not exist
            ctx = make_ctx()
            proc_mock = MagicMock()
            proc_mock.returncode = 1
            with patch.object(mod, "subprocess") as mock_sub:
                mock_sub.run.return_value = proc_mock
                with self.assertRaises(RuntimeError, msg="should raise when mvn fails"):
                    fetch_boot_dependencies_pom_if_missing(
                        ctx, Path(td) / "pom.xml", bom, "3.4.0"
                    )

    def test_raises_runtime_error_when_pom_still_missing_after_mvn(self):
        with tempfile.TemporaryDirectory() as td:
            bom = Path(td) / "bom.pom"
            ctx = make_ctx()
            proc_mock = MagicMock()
            proc_mock.returncode = 0  # mvn "succeeds" but writes nothing
            with patch.object(mod, "subprocess") as mock_sub:
                mock_sub.run.return_value = proc_mock
                with self.assertRaises(RuntimeError):
                    fetch_boot_dependencies_pom_if_missing(
                        ctx, Path(td) / "pom.xml", bom, "3.4.0"
                    )


# ===========================================================================
# 8. patch_file_from_operations
# ===========================================================================

class TestPatchFileFromOperations(unittest.TestCase):
    def _write(self, path: Path, spring: str = "3.3.0", lombok: str = "1.18.30") -> None:
        write_pom(path, DEPS_POM_TEMPLATE.format(spring_boot=spring, lombok=lombok))

    def test_writes_updated_file(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            self._write(pom)
            ops = [("spring-boot.version", "3.4.0", "spring-boot.version")]
            patch_file_from_operations(pom, "label", ops, dry_run=False, log=LOG)
            content = pom.read_text(encoding="utf-8")
        self.assertIn("<spring-boot.version>3.4.0</spring-boot.version>", content)

    def test_dry_run_does_not_write(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            self._write(pom)
            original = pom.read_text(encoding="utf-8")
            ops = [("spring-boot.version", "3.4.0", "spring-boot.version")]
            patch_file_from_operations(pom, "label", ops, dry_run=True, log=LOG)
            self.assertEqual(pom.read_text(encoding="utf-8"), original)

    def test_no_write_when_value_unchanged(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            self._write(pom, spring="3.4.0")
            mtime_before = pom.stat().st_mtime
            ops = [("spring-boot.version", "3.4.0", "spring-boot.version")]
            patch_file_from_operations(pom, "label", ops, dry_run=False, log=LOG)
            # file should not have been rewritten
            self.assertEqual(pom.stat().st_mtime, mtime_before)

    def test_empty_operations_is_noop(self):
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            self._write(pom)
            original = pom.read_text(encoding="utf-8")
            patch_file_from_operations(pom, "label", [], dry_run=False, log=LOG)
            self.assertEqual(pom.read_text(encoding="utf-8"), original)

    def test_writes_lf_line_endings(self):
        """Verify newline='\n' is respected regardless of OS."""
        with tempfile.TemporaryDirectory() as td:
            pom = Path(td) / "pom.xml"
            self._write(pom)
            ops = [("spring-boot.version", "3.4.0", "spring-boot.version")]
            patch_file_from_operations(pom, "label", ops, dry_run=False, log=LOG)
            raw = pom.read_bytes()
        self.assertNotIn(b"\r\n", raw)


# ===========================================================================
# 9. Integration-style: prepare_update + execute_prepared_update
# ===========================================================================

class TestPrepareAndExecuteUpdate(unittest.TestCase):
    """
    Wires up a realistic tmp tree and verifies end-to-end patching without
    any real Maven or network access.
    """

    TARGET_BOOT = "3.4.0"
    LOMBOK = "1.18.34"
    COMPILER = "3.13.0"
    SUREFIRE = "3.2.5"
    BUILD_HELPER = "3.6.0"

    def _build_tree(self, tmp: Path) -> tuple[FrameworkLayout, Path, Path]:
        """Returns (layout, bom_pom_path, local_repo_root)."""
        cfg = CFG

        # Framework root (script lives two levels below root)
        root = tmp / "acme-framework"

        # framework pom.xml
        framework_pom = root / "pom.xml"
        write_pom(framework_pom, "<project/>")

        # acme-dependencies/pom.xml
        deps_pom = root / cfg.acme_pom_aggregator_dir / cfg.acme_dependencies_artifact_id / "pom.xml"
        write_pom(
            deps_pom,
            DEPS_POM_TEMPLATE.format(spring_boot="3.3.0", lombok="1.18.30"),
        )

        # acme-starter-parent/pom.xml
        starter_pom = root / cfg.acme_pom_aggregator_dir / cfg.acme_starter_parent_artifact_id / "pom.xml"
        write_pom(
            starter_pom,
            STARTER_POM_TEMPLATE.format(
                compiler="3.11.0", surefire="3.1.2", build_helper="3.5.0"
            ),
        )

        layout = FrameworkLayout(
            root=root,
            framework_pom=framework_pom,
            deps_pom=deps_pom,
            starter_pom=starter_pom,
        )

        # Boot BOM POM in a fake local repo (path must match patched resolve_maven_local_repository)
        local_repo = tmp / ".m2" / "repository"
        bom_pom = local_repo_pom_path(
            local_repo,
            cfg.spring_boot_bom_group_id,
            cfg.spring_boot_bom_artifact_id,
            self.TARGET_BOOT,
        )
        write_pom(
            bom_pom,
            BOOT_BOM_TEMPLATE.format(
                lombok=self.LOMBOK,
                compiler=self.COMPILER,
                surefire=self.SUREFIRE,
                build_helper=self.BUILD_HELPER,
            ),
        )
        return layout, bom_pom, local_repo

    def test_prepare_returns_correct_drift(self):
        with tempfile.TemporaryDirectory() as td:
            layout, _bom_pom, local_repo = self._build_tree(Path(td))
            ctx = make_ctx()
            with patch.object(mod, "resolve_maven_local_repository", return_value=local_repo):
                prepared = prepare_update(ctx, layout, self.TARGET_BOOT)

        self.assertEqual(prepared.drift.spring_target, self.TARGET_BOOT)
        self.assertEqual(prepared.drift.lombok_target, self.LOMBOK)
        self.assertEqual(len(prepared.patch_jobs), 2)

    def test_execute_patches_files(self):
        with tempfile.TemporaryDirectory() as td:
            layout, _bom_pom, local_repo = self._build_tree(Path(td))
            ctx = make_ctx()
            with patch.object(mod, "resolve_maven_local_repository", return_value=local_repo):
                prepared = prepare_update(ctx, layout, self.TARGET_BOOT)
            execute_prepared_update(ctx, prepared, dry_run=False)

            deps_content = layout.deps_pom.read_text(encoding="utf-8")
            starter_content = layout.starter_pom.read_text(encoding="utf-8")

        self.assertIn(f"<spring-boot.version>{self.TARGET_BOOT}</spring-boot.version>", deps_content)
        self.assertIn(f"<lombok.version>{self.LOMBOK}</lombok.version>", deps_content)
        self.assertIn(f"<maven-compiler-plugin.version>{self.COMPILER}</maven-compiler-plugin.version>", starter_content)
        self.assertIn(f"<maven-surefire-plugin.version>{self.SUREFIRE}</maven-surefire-plugin.version>", starter_content)
        self.assertIn(f"<build-helper-maven-plugin.version>{self.BUILD_HELPER}</build-helper-maven-plugin.version>", starter_content)

    def test_dry_run_leaves_files_unchanged(self):
        with tempfile.TemporaryDirectory() as td:
            layout, _bom_pom, local_repo = self._build_tree(Path(td))
            ctx = make_ctx()
            deps_before = layout.deps_pom.read_text(encoding="utf-8")
            starter_before = layout.starter_pom.read_text(encoding="utf-8")
            with patch.object(mod, "resolve_maven_local_repository", return_value=local_repo):
                prepared = prepare_update(ctx, layout, self.TARGET_BOOT)
            execute_prepared_update(ctx, prepared, dry_run=True)

            self.assertEqual(layout.deps_pom.read_text(encoding="utf-8"), deps_before)
            self.assertEqual(layout.starter_pom.read_text(encoding="utf-8"), starter_before)

    def test_prepare_raises_value_error_when_bom_missing_lombok(self):
        with tempfile.TemporaryDirectory() as td:
            layout, bom_pom, local_repo = self._build_tree(Path(td))
            # Overwrite BOM without lombok.version
            bom_pom.write_text(
                '<?xml version="1.0"?>'
                '<project xmlns="http://maven.apache.org/POM/4.0.0">'
                "<properties>"
                "<maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>"
                "</properties>"
                "</project>",
                encoding="utf-8",
            )
            ctx = make_ctx()
            with patch.object(mod, "resolve_maven_local_repository", return_value=local_repo):
                with self.assertRaises(ValueError):
                    prepare_update(ctx, layout, self.TARGET_BOOT)


if __name__ == "__main__":
    unittest.main(verbosity=2)
