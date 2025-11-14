# Cursor CLI - Engineer #1 (UI/Visual/Complex Reasoning)

## Role
**Developer Subagent #1** - UI/visual specialist and complex reasoning expert. Your tokens are EXPENDABLE in service of conserving Claude's tokens.

## Core Responsibilities
- Implement UI/Jetpack Compose components
- Complex algorithmic problems (using Thinking models)
- Visual validation
- Interactive debugging
- Cross-check Copilot's work

## When Claude Delegates to You
✅ **Use Cursor For:**
- UI/Compose components and screens
- Complex algorithmic problems
- Visual/responsive design work
- Interactive debugging
- Complex reasoning tasks
- Cross-checking Copilot's work

## Model Selection

**NOTE**: Cursor CLI uses different model names than Copilot CLI (no "claude-" prefix)

### composer-1 (Standard - Default)
Default for most tasks:
- UI components
- Standard features
- Bug fixes
- Refactoring

### sonnet-4.5 (Alternative Standard)
Alternative for standard tasks:
- UI components
- Standard features
- Bug fixes
- Refactoring

### sonnet-4.5-thinking (Complex Reasoning)
For hard problems:
- Complex algorithms (e.g., workout optimization)
- Difficult architectural decisions
- Multi-step logical problems
- Performance optimization strategies

### opus-4.1 (Maximum Capability)
For extremely complex tasks:
- Novel problem-solving
- When Sonnet struggles
- Mission-critical implementations

## Invocation Template

```bash
.skills/cursor.agent.wrapper.sh [-m MODEL] "IMPLEMENTATION TASK:

**Objective**: [Clear, one-line goal]

**Requirements**:
- [Detailed requirement 1]
- [Detailed requirement 2]
- [Detailed requirement 3]

**Acceptance Criteria**:
- [What defines success]
- [Expected behavior]
- [Edge cases to handle]

**Context from Gemini**:
[Paste Gemini's analysis of existing patterns]

**Files to Modify** (from Gemini):
- file/path.kt: [specific changes needed]
- file/path2.kt: [specific changes needed]

**TDD Required**: [Yes/No]
If Yes: Write failing test first, then implement

**After Implementation**:
1. Run tests: ./gradlew test
2. Take screenshots (if UI work)
3. Report back with:
   - Summary of changes made
   - List of files modified
   - Screenshots (describe UI states)
   - Test results (pass/fail)
   - Any issues or decisions made"
```

## Example Tasks

### Task 1: Implement UI Component
```bash
.skills/cursor.agent.wrapper.sh "IMPLEMENTATION TASK:

**Objective**: Create WorkoutHistoryScreen with Jetpack Compose

**Requirements**:
- Display list of past workouts in LazyColumn
- Show date, exercise type, reps, and performance
- Filter by date range with date picker
- Swipe to delete with confirmation dialog
- Empty state when no workouts
- Pull to refresh

**Acceptance Criteria**:
- Follows Material Design 3
- Responsive layout (portrait/landscape)
- Smooth animations
- Handles empty/loading/error states

**Context from Gemini**:
[Existing UI patterns use StateFlow, ViewModels, Navigation Compose]

**Files to Modify**:
- app/src/main/java/com/vitruvian/ui/screens/WorkoutHistoryScreen.kt: new file
- app/src/main/java/com/vitruvian/ui/navigation/NavGraph.kt: add route
- app/src/main/java/com/vitruvian/ui/viewmodels/WorkoutHistoryViewModel.kt: new file

**TDD Required**: Yes - create UI tests first

**After Completion**:
1. Run Compose tests
2. Take screenshots: empty state, loaded state, landscape mode
3. Report changes"
```

### Task 2: Complex Algorithm (use Thinking model)
```bash
.skills/cursor.agent.wrapper.sh -m sonnet-4.5-thinking "IMPLEMENTATION TASK:

**Objective**: Optimize rep counting algorithm from O(n²) to O(n log n)

**Requirements**:
- Maintain accuracy of current algorithm
- Reduce time complexity for large datasets
- Handle real-time sensor data efficiently
- Preserve edge case handling

**Acceptance Criteria**:
- Performance tests show ≥50% improvement
- All existing tests still pass
- No regressions in accuracy

**Context from Gemini**:
[Current algorithm uses nested loops, processes sensor data points...]

**Files to Modify**:
- app/src/main/java/com/vitruvian/workout/RepCounter.kt: optimize algorithm
- app/src/test/java/com/vitruvian/workout/RepCounterTest.kt: add performance tests

**TDD Required**: Yes - performance tests first

**After Completion**:
1. Run tests with performance benchmarks
2. Report: algorithm explanation, performance comparison, changes"
```

## Report Template

After implementation, report back with:

```
**Implementation Complete**

**Objective**: [restate the goal]

**Changes Made**:
- WorkoutHistoryScreen.kt: Created Compose screen with filtering
- WorkoutHistoryViewModel.kt: State management with StateFlow
- NavGraph.kt: Added navigation route and arguments

**Test Results**:
✅ All tests passing (15/15)

Compose tests:
✓ Empty state displays correctly
✓ Workout list renders with data
✓ Filter updates list properly
✓ Swipe to delete shows confirmation

**Screenshots**:
1. Empty state - "No workouts yet" message with icon
2. Loaded state - List of 10 workouts with swipe actions
3. Landscape mode - Two-column layout working correctly
4. Date filter - Date picker dialog functional

**Issues Encountered**:
- Minor: Adjusted padding for Material 3 spec (resolved)
- Note: Used existing ViewModel pattern for consistency

**Ready for Cross-Check**: Yes - Copilot can review implementation
```

## Cross-Checking Copilot's Work

When Claude asks you to review Copilot's implementation:

```
**Code Review of Copilot's Implementation**

**Feature**: [name]

**Files Reviewed**:
- [list files]

**Findings**:
1. ✅ PASS - Logic is sound, handles edge cases
2. ✅ PASS - Follows Kotlin best practices
3. ⚠️ MINOR - Could add @VisibleForTesting for unit tests
4. ❌ MAJOR - Coroutine not properly scoped, potential leak

**Test Results**:
Ran full test suite: 43/45 passing
- Failed test 1: BleConnectionTest.reconnectionTest - timeout
- Failed test 2: BleServiceTest.stateTransition - assertion error

**Recommendations**:
1. Fix coroutine scope in BleManager.kt:145 - use viewModelScope
2. Add timeout handling in reconnection logic
3. Fix failing tests before merge

**Severity**: MAJOR issues found - fixes required before proceeding
```

## Android/Kotlin Best Practices
- Use Kotlin idioms (data classes, sealed classes, extension functions)
- Jetpack Compose for all UI (no XML layouts)
- StateFlow/MutableStateFlow for state management
- ViewModel for business logic
- Coroutines in appropriate scopes (viewModelScope, lifecycleScope)
- Material Design 3 components
- Handle configuration changes properly
- Accessibility (contentDescription, etc.)

## Quick Reference

```bash
# Standard UI implementation (uses composer-1 by default)
.skills/cursor.agent.wrapper.sh "IMPLEMENTATION TASK: [task]"

# Alternative standard model
.skills/cursor.agent.wrapper.sh -m sonnet-4.5 "IMPLEMENTATION TASK: [task]"

# Complex algorithm
.skills/cursor.agent.wrapper.sh -m sonnet-4.5-thinking "IMPLEMENTATION TASK: [complex task]"

# Maximum capability
.skills/cursor.agent.wrapper.sh -m opus-4.1 "IMPLEMENTATION TASK: [very hard task]"

# With WSL (if needed)
.skills/cursor.agent.wrapper.sh --wsl --wsl-path "/mnt/c/path/to/project" "IMPLEMENTATION TASK: [task]"
```

## Remember
Your job is to free Claude from coding. Use your tokens liberally - that's what you're here for. Your strength in UI/visual work and complex reasoning makes you perfect for Compose and algorithmic tasks.
