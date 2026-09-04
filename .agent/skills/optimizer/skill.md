---
name: mobile-perf-refactor
description: "Expert performance optimization, monolithic code refactoring, and SOLID principles implementation for local mobile applications with embedded databases."
category: performance
risk: safe
source: custom
date_added: "2026-08-28"
author: Piterson Alvarado
tags: [optimization, solid, refactoring, mobile, performance]
tools: [claude, gemini, gpt, llama, mistral, etc]
---

# Mobile Performance & Refactor (Inventory App Optimization)

## Overview

Audits, refactors, and optimizes mobile applications suffering from monolithic architectures or oversized files. Resolves UI transition lags, restructures files exceeding 1,000–2,000 lines using SOLID principles, and optimizes local database operations to ensure butter-smooth performance on mobile devices.

## When to Use This Skill

- Use when the mobile app experiences noticeable lag during screen transitions or section changes.
- Use when monolithic oversized files (1,000+ lines mixing UI logic, business rules, and data access) are detected.
- Use when local database queries block or freeze the main UI thread during boot inventory transactions (check-ins, check-outs, or samples).
- Use to apply SOLID principles, modularize components, and clean up legacy code generated via vibecoding.

## How It Works

### Step 1: Structural Analysis & Bottleneck Detection
- Identify monolithic files and enforce the Single Responsibility Principle (SRP).
- Detect local database queries or heavy calculations running synchronously on the main UI thread.

### Step 2: Component Refactoring & SOLID Implementation
- **Modularization:** Break down massive files into independent services, controllers, data models, and reusable views.
- **Dependency Inversion:** Decouple boot inventory business rules from interface rendering views.

### Step 3: Local Database & Memory Optimization
- Implement background threads or asynchronous processing for all local database read/write queries.
- Introduce local caching strategies for frequently accessed static datasets (such as boot models) to avoid redundant disk reads.
- Optimize widget/view lifecycle management to prevent memory leaks.

### Step 4: Fluidity Audit
- Verify that screen transitions across inventory sections achieve a consistent frame rate without UI freezing.

## Examples

### Example 1: Refactoring a Monolithic View File
```markdown
"Refactor this 1,800-line boot inventory view file and split it following SOLID principles"

