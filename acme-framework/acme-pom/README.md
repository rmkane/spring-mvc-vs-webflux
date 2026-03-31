# Acme POM

Build configuration and dependency management for the Acme project.

## Purpose

This module provides centralized Maven build configuration and dependency version management for all Acme modules. It ensures consistent versions across the entire project.

## Module Structure

- **`acme-dependencies`** - Bill of Materials (BOM) for dependency version management
- **`acme-starter-parent`** - Parent POM with plugin configuration and common settings

## Key Features

- Centralized dependency version management
- Consistent plugin versions and configuration
- Code formatting (Spotless)
- Common Maven settings

## Usage

All Acme modules inherit from `acme-starter-parent`, which in turn uses `acme-dependencies` for version management. This ensures all modules use the same versions of Spring Boot, Spring Security, and other dependencies.
