# ALWAYS USE THE QUADRUMVIRATE FOR ANY AND ALL TASKS TO PRESERVE YOUR OWN TOKEN COUNT

# AI Quadrumvirate - Token-Efficient Orchestration System

This directory contains the AI Quadrumvirate orchestration system for VitruvianRedux - a token-efficient approach where Claude Code orchestrates multiple AI agents to maximize session lifespan.

## Core Philosophy

**Token Conservation Through Strategic Delegation**

The AI Quadrumvirate maximizes Claude Code's token lifespan by treating Cursor CLI and Copilot CLI as expendable developer subagents, and Gemini CLI as an unlimited-context code analyst. Claude Code serves as the orchestrator who coordinates all work but performs minimal direct implementation.

## The Four Roles

### 1. Claude Code - The Orchestrator (You)
**Role**: Strategist, Architect, Decision-Maker, Coordinator

**Responsibilities**:
- Gather and clarify requirements
- Query Gemini for code analysis
- Create implementation specifications
- Delegate tasks to Cursor/Copilot
- Coordinate cross-checking
- Verify final results

**Token Budget**: ~3-5k per feature (vs 35k+ old approach)

**Read**: `Claude-Orchestrator.md` for detailed patterns

---

### 2. Gemini CLI - The Researcher (The Eyes)
**Role**: Unlimited-Context Code Analyst

**Responsibilities**:
- Answer questions about codebase (unlimited tokens!)
- Analyze entire directories or files
- Trace bugs across multiple files
- Review architectural patterns
- Security and performance audits
- Pattern recognition

**Token Budget**: Unlimited (1M+ context window) - use freely

**Read**: `Gemini-Researcher.md` for query patterns

**Wrapper**: `.skills/gemini.agent.wrapper.sh`

---

### 3. Cursor CLI - Engineer #1
**Role**: UI/Visual Implementation, Complex Reasoning

**Responsibilities**:
- Implement UI/Jetpack Compose components
- Complex algorithmic problems (using Thinking models)
- Visual validation
- Interactive debugging
- Cross-check Copilot's work

**Token Budget**: Expendable - use for all UI/complex work

**Read**: `Cursor-Engineer.md` for task templates

**Wrapper**: `.skills/cursor.agent.wrapper.sh`

---

### 4. Copilot CLI - Engineer #2
**Role**: Backend/BLE/Services/GitHub Operations

**Responsibilities**:
- Backend services and repositories
- BLE operations and connection management
- Database operations (Room)
- GitHub operations (PRs, issues)
- Git operations
- Cross-check Cursor's work

**Token Budget**: Expendable - use for all backend/GitHub work

**Read**: `Copilot-Engineer.md` for task templates

**Wrapper**: `.skills/copilot.agent.wrapper.sh`

---

## Quick Start

### Example: Implement New Feature

#### Step 1: Ask Gemini for Analysis
```bash
.skills/gemini.agent.wrapper.sh -d "@app/src/" "
Feature: Add workout history screen with filtering

Questions:
1. What existing UI patterns should I follow?
2. Where is workout data currently stored?
3. What files will be affected?
4. Recommended implementation approach?

Provide file paths and code excerpts."
```

#### Step 2: Delegate to Cursor (UI)
```bash
.skills/cursor.agent.wrapper.sh "IMPLEMENTATION TASK:

**Objective**: Create WorkoutHistoryScreen with Jetpack Compose

**Requirements**:
- Display list of past workouts
- Filter by date range
- Swipe to delete

**Context from Gemini**:
[paste Gemini's response]

**Files to Modify**:
- app/src/main/java/com/vitruvian/ui/screens/WorkoutHistoryScreen.kt: new
- app/src/main/java/com/vitruvian/ui/navigation/NavGraph.kt: add route

**After Completion**:
1. Run tests
2. Take screenshots
3. Report changes"
```

#### Step 3: Delegate to Copilot (Backend)
```bash
.skills/copilot.agent.wrapper.sh --allow-write "IMPLEMENTATION TASK:

**Objective**: Create Room database queries for workout history

**Requirements**:
- Query workouts by date range
- Support filtering by exercise type
- Paginated results

**Context from Gemini**:
[paste Gemini's response]

**Files to Modify**:
- app/src/main/java/com/vitruvian/data/local/WorkoutDao.kt: add queries

**After Completion**:
1. Run tests
2. Report changes"
```

#### Step 4: Cross-Check
```bash
# Copilot reviews Cursor's UI work
.skills/copilot.agent.wrapper.sh "CODE REVIEW:
Review Cursor's WorkoutHistoryScreen implementation.
Check: logic, integration, tests, performance."

# Cursor reviews Copilot's backend work
.skills/cursor.agent.wrapper.sh "CODE REVIEW:
Review Copilot's WorkoutDao queries.
Check: correctness, edge cases, tests."
```

#### Step 5: Verify with Gemini
```bash
.skills/gemini.agent.wrapper.sh -d "@app/src/" "
Changes implemented:
- WorkoutHistoryScreen.kt: new Compose screen
- WorkoutDao.kt: new queries

Verify:
1. Architectural consistency
2. No regressions
3. Best practices followed
4. Performance acceptable"
```

**Claude's Token Usage**: ~3k (orchestration only)
**Gemini/Cursor/Copilot**: Handle all analysis and implementation

---

## Wrapper Scripts

All three CLIs have wrapper scripts for convenient invocation:

### Gemini Wrapper
```bash
.skills/gemini.agent.wrapper.sh [OPTIONS] "<prompt>"

Options:
  -d, --dir "@path/"      Directories to analyze
  -a, --all-files         Analyze entire codebase
  -o, --output json       Output format (text, json)
```

### Cursor Wrapper
```bash
.skills/cursor.agent.wrapper.sh [OPTIONS] "<prompt>"

Options:
  -m, --model MODEL       composer-1 (default), sonnet-4.5, sonnet-4.5-thinking, opus-4.1
  --wsl                   Use WSL execution
```

### Copilot Wrapper
```bash
.skills/copilot.agent.wrapper.sh [OPTIONS] "<prompt>"

Options:
  --allow-write           Allow file writes
  --allow-git             Allow git operations
  --allow-github          Allow GitHub operations
```

---

## Token Savings Examples

### Feature Development
| Approach | Claude Tokens | Savings |
|----------|--------------|---------|
| Old (Claude does everything) | 35k | - |
| New (Quadrumvirate) | 3-5k | **86-91%** |

### Bug Investigation
| Approach | Claude Tokens | Savings |
|----------|--------------|---------|
| Old | 28k | - |
| New | 2k | **93%** |

### Code Review
| Approach | Claude Tokens | Savings |
|----------|--------------|---------|
| Old | 28k | - |
| New | 1k | **96%** |

---

## Success Criteria

You're using the Quadrumvirate correctly when:
1. ✅ Claude's token usage is <5k per task
2. ✅ Gemini is queried before implementation
3. ✅ Cursor/Copilot do all implementation work
4. ✅ Developers cross-check each other
5. ✅ Superpowers skills are used for structure
6. ✅ TodoWrite tracks progress
7. ✅ Final verification uses Gemini + minimal Claude reads

---

## Project Context: VitruvianRedux

**Android Kotlin App** for fitness equipment management:
- **Language**: Kotlin with Coroutines
- **UI**: Jetpack Compose
- **Architecture**: MVVM
- **BLE**: BluetoothGatt for device communication
- **Database**: Room
- **Build**: Gradle with Kotlin DSL

**Core Features**:
- BLE connectivity to Vitruvian devices
- Real-time workout tracking with rep counting
- Auto-stop based on performance metrics
- Notification system for workout feedback

---

## Files in This Directory

- **Claude-Orchestrator.md** - Your role as orchestrator with detailed delegation patterns
- **Gemini-Researcher.md** - Gemini's role as unlimited-context analyst
- **Cursor-Engineer.md** - Cursor's role as UI/visual/complex reasoning specialist
- **Copilot-Engineer.md** - Copilot's role as backend/BLE/GitHub specialist
- **gemini.agent.wrapper.sh** - Gemini CLI wrapper script
- **cursor.agent.wrapper.sh** - Cursor CLI wrapper script
- **copilot.agent.wrapper.sh** - Copilot CLI wrapper script
- **README.md** - This file

---

## Integration with Claude Code

This system is designed to work seamlessly with Claude Code's native features:

- **Superpowers Skills**: Use for structured workflows (brainstorming, TDD, systematic-debugging, etc.)
- **TodoWrite**: Track progress and coordinate multi-step tasks
- **Task Tool**: Can still use for subagent delegation when appropriate
- **CLAUDE.md**: Session start protocol and token conservation rules

---

## Summary

**The New Philosophy**:
- **Claude**: Conductor who never plays instruments directly
- **Gemini**: Unlimited knowledge base, always consulted
- **Cursor/Copilot**: Expendable developers who do the work
- **Result**: 80-90% token savings, 8-10x session lifespan

**Remember**: Your value is in orchestration and decision-making, not in reading files or writing code. Delegate everything except strategic thinking.

**Target**: <5k tokens per feature, enabling 40+ features per 200k token session (vs 5-7 in old approach).
