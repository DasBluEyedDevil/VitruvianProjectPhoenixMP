# Cursor CLI Wrapper - Fixed Usage Guide

## Issue That Was Fixed

**Problem:** When passing prompts with code blocks directly to the wrapper via bash, the code blocks would be interpreted by bash before reaching cursor-agent, causing syntax errors and execution failures.

**Example of problematic usage:**
```bash
bash .skills/cursor.agent.wrapper.sh "Task with code:
```kotlin
val foo = bar
```
"
# This fails because bash tries to execute the kotlin code!
```

## Solution: Prompt File Option

Added `--prompt-file` / `-f` option to read prompts from files, completely avoiding bash escaping issues.

## Correct Usage Patterns

### ✅ Method 1: Use Prompt File (RECOMMENDED for complex prompts)

```bash
# 1. Create prompt file with your task
cat > /tmp/cursor_task.txt << 'EOF'
PHASE 2 HIGH PRIORITY - Material Design 3 Theming

## Task
Migrate all hardcoded colors to MaterialTheme.colorScheme.

## Mapping
```kotlin
// Before → After
PrimaryPurple → MaterialTheme.colorScheme.primary
SurfaceDarkGrey → MaterialTheme.colorScheme.surface
```

## Files to Update
- ExerciseEditDialog.kt
- WorkoutTab.kt
EOF

# 2. Invoke wrapper with file
bash .skills/cursor.agent.wrapper.sh -f /tmp/cursor_task.txt
```

### ✅ Method 2: Simple String (for short prompts without code)

```bash
bash .skills/cursor.agent.wrapper.sh "Implement haptic feedback in RepCounterCard component"
```

### ❌ Method 3: AVOID - Direct code blocks in string

```bash
# DON'T DO THIS:
bash .skills/cursor.agent.wrapper.sh "Task: update colors
```kotlin
val foo = bar
```
"
```

## Usage from Claude Code Agent Tool

When using the Bash tool to invoke Cursor wrapper:

**BAD:**
```xml
<invoke name="Bash">
<parameter name="command">bash .skills/cursor.agent.wrapper.sh "Task with ```kotlin code``` blocks"