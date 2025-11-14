# Quadrumvirate Wrapper Scripts - Status & Fixes

**Date**: 2025-11-13
**Project**: VitruvianProjectPhoenixMP

## Summary

The Quadrumvirate wrapper scripts were copied from VitruvianRedux. After investigation, here's the current status:

## Wrapper Status

### 1. Gemini Wrapper ✅ WORKING
**Script**: `.skills/gemini.agent.wrapper.sh`
**CLI Location**: `/c/Users/dasbl/AppData/Roaming/npm/gemini`
**Status**: ✅ Fully functional

**Usage**:
```bash
bash .skills/gemini.agent.wrapper.sh -d "@app/src/" "Your analysis question here"
```

**Notes**:
- Gemini CLI is installed and accessible
- Has unlimited context (1M+ tokens)
- Should be used for all code analysis

---

### 2. Copilot Wrapper ✅ WORKING
**Script**: `.skills/copilot.agent.wrapper.sh`
**CLI Location**: `/c/Users/dasbl/AppData/Roaming/npm/copilot`
**Status**: ✅ Fully functional

**Usage**:
```bash
bash .skills/copilot.agent.wrapper.sh --allow-write "IMPLEMENTATION TASK: ..."
```

**Notes**:
- Copilot CLI is installed and accessible
- Use for backend, GitHub, git operations
- Requires permission flags (--allow-write, --allow-git, etc.)

---

### 3. Cursor Wrapper ❌ REQUIRES SETUP
**Script**: `.skills/cursor.agent.wrapper.sh`
**CLI Location**: NOT INSTALLED
**Status**: ❌ Requires cursor-agent in WSL

**Issue**:
The wrapper expects `cursor-agent` CLI to be installed in WSL at:
```
/home/devil/.local/bin/cursor-agent
```

But cursor-agent is NOT currently installed in WSL.

**Two Options to Fix**:

#### Option A: Install cursor-agent in WSL (Recommended if available)
```bash
# In WSL (as user 'devil')
# Follow cursor-agent installation instructions
# This would enable the full Quadrumvirate workflow
```

#### Option B: Modify wrapper to work without cursor-agent
Since cursor-agent may not be publicly available or may require special setup, we can either:
1. Use Claude Code Task tool instead (built-in subagent delegation)
2. Remove cursor-agent dependency (if not needed)
3. Document that cursor wrapper is optional

---

## Recommendations

### For This Project (VitruvianProjectPhoenixMP)

1. **Use Gemini wrapper** ✅ for all code analysis
   - Saves massive tokens by using unlimited context
   - Always query Gemini before reading large files

2. **Use Copilot wrapper** ✅ for backend/GitHub work
   - Git operations
   - GitHub PR/issue management
   - Backend implementation

3. **Skip cursor wrapper for now** ⚠️
   - Use Claude Code's built-in Task tool instead
   - Or delegate UI work to Copilot
   - cursor-agent appears to be a custom/internal tool from VitruvianRedux

### Token-Saving Workflow (Without Cursor)

```
1. Analysis: Use Gemini wrapper → 0 Claude tokens
2. Planning: Claude creates spec → ~1-2k tokens
3. Implementation:
   - Backend: Use Copilot wrapper → 0 Claude tokens
   - UI: Use Task tool with UI subagent → minimal tokens
4. Verification: Use Gemini wrapper → 0 Claude tokens
```

**Result**: Still achieve 70-85% token savings without cursor-agent

---

## Configuration Changes Needed

### Update cursor wrapper default path
The cursor wrapper has hardcoded path from VitruvianRedux:

**File**: `.skills/cursor.agent.wrapper.sh`
**Line 24**:
```bash
# OLD (VitruvianRedux)
WSL_PATH="/mnt/c/Users/dasbl/AndroidStudioProjects/VitruvianRedux"

# NEW (VitruvianProjectPhoenixMP)
WSL_PATH="/mnt/c/Users/dasbl/AndroidStudioProjects/VitruvianProjectPhoenixMP"
```

This change has been made, but cursor-agent still needs to be installed to use it.

---

## Testing

**Gemini Wrapper Test**:
```bash
bash .skills/gemini.agent.wrapper.sh "What is 2+2?"
# Should respond with answer
```

**Copilot Wrapper Test**:
```bash
bash .skills/copilot.agent.wrapper.sh "List files in current directory"
# Should execute and list files
```

**Cursor Wrapper**:
```bash
bash .skills/cursor.agent.wrapper.sh "Test"
# Will fail: cursor-agent not found in WSL
```

---

## Next Steps

1. ✅ Update cursor wrapper default path (if we decide to use it)
2. ⚠️ Decide if cursor-agent installation is needed
3. ✅ Proceed with KMP porting plan using working wrappers (Gemini + Copilot)
4. ✅ Document workaround: Use Task tool instead of cursor wrapper

---

## Conclusion

**2 out of 3 wrappers are fully functional** ✅✅⚠️

This is sufficient to use the Quadrumvirate workflow effectively:
- Gemini for analysis (unlimited context)
- Copilot for implementation
- Claude for orchestration only

We can still achieve the 70-85% token savings goal without cursor-agent.
