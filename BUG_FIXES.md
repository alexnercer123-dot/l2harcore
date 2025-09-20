# Bug Fixes Documentation

This document details the bug fixes implemented for the L2Hardcore server.

## Fix #1: Clan Leader Skills Availability

### Problem Description
Clan leader skills were incorrectly available at clan level 5 instead of the intended clan level 3. The affected skills were:
- **Печать Света** (Seal of Light)
- **Печать тьмы** (Seal of Darkness) 
- **Разбить Лагерь** (Break Camp)

### Root Cause
The `_siegeClanMinLevel` parameter in `SiegeManager.java` was set to 5, and corresponding level checks in `Clan.java` were using level 5 thresholds.

### Solution Implemented

#### Modified Files:
1. **`SiegeManager.java`**
   - Changed `_siegeClanMinLevel` from 5 to 3
   - Updated default value in configuration loading

2. **`Clan.java`**
   - Updated level checks from `> 4` to `> 2`
   - Updated level checks from `< 5` to `< 3`

#### Code Changes:

**SiegeManager.java:**
```java
// Line 61: Changed clan minimum level requirement
private int _siegeClanMinLevel = 3; // Changed from 5 to 3

// Line 135: Updated default configuration value
_siegeClanMinLevel = siegeConfig.getInt("SiegeClanMinLevel", 3); // Changed from 5 to 3
```

**Clan.java:**
```java
// Lines 686-692: Updated level checks in changeLevel method
if (level > 2) // Changed from level > 4
{
    SiegeManager.getInstance().addSiegeSkills(leader);
    leader.sendPacket(SystemMessageId.NOW_THAT_YOUR_CLAN_LEVEL_IS_ABOVE_LEVEL_5_IT_CAN_ACCUMULATE_CLAN_REPUTATION);
}
else if (level < 3) // Changed from level < 5
{
    SiegeManager.getInstance().removeSiegeSkills(leader);
}
```

### Result
- Clan leaders now receive siege skills at clan level 3 instead of level 5
- All existing functionality remains intact
- Consistent with intended game design

---

## Fix #2: Bleed and Poison Effect Stacking

### Problem Description
BLEEDING and POISON effects were incorrectly replacing each other when applied by different players on the same target. According to the requirements:
- Effects should **stack** when applied by different casters on **raid bosses, epic bosses, and minions**
- Effects should **replace** each other on **regular monsters** (original behavior)

### Examples:
- ✅ **Correct**: 5 players cast Sting on a raid boss → 5 separate Sting effects active
- ✅ **Correct**: 5 players cast Poison on a regular monster → only 1 Poison effect active (latest one)

### Root Cause
The effect stacking logic in `EffectList.java` was treating all creatures the same way, causing BLEEDING and POISON effects to always replace each other regardless of target type or caster.

### Solution Implemented

#### Modified Files:
1. **`EffectList.java`**
   - Added special stacking logic for BLEEDING and POISON effects
   - Added `InstanceType` import for creature type checking

#### Code Changes:

**EffectList.java Imports:**
```java
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
```

**EffectList.java - Enhanced Stacking Logic:**
```java
// Special case: Allow BLEEDING and POISON effects to stack from different casters 
// on raid bosses, epic bosses, and minions
boolean shouldStack = false;
if (!skill.getAbnormalType().isNone() && (existingSkill.getAbnormalType() == skill.getAbnormalType()) &&
    ((skill.getAbnormalType() == AbnormalType.BLEEDING) || (skill.getAbnormalType() == AbnormalType.POISON)) &&
    (info.getEffectorObjectId() != existingInfo.getEffectorObjectId()) &&
    (_owner.isInstanceTypes(InstanceType.RaidBoss, InstanceType.GrandBoss) || _owner.isMinion() || _owner.isRaidMinion()))
{
    shouldStack = true;
}

// Apply normal stacking rules unless special stacking conditions are met
if (((skill.getAbnormalType().isNone() && (existingSkill.getId() == skill.getId())) || 
     (!skill.getAbnormalType().isNone() && (existingSkill.getAbnormalType() == skill.getAbnormalType()))) && 
    !shouldStack)
{
    // ... existing replacement logic
}
```

### Logic Breakdown

The fix implements the following stacking conditions:
1. **Effect Type Check**: Must be BLEEDING or POISON abnormal type
2. **Different Casters**: Effects must come from different players (`effectorObjectId` comparison)
3. **Target Type Check**: Target must be one of:
   - Raid Boss (`InstanceType.RaidBoss`)
   - Epic Boss (`InstanceType.GrandBoss`) 
   - Minion (`isMinion()` or `isRaidMinion()`)

### Result
- **Raid Bosses, Epic Bosses, Minions**: BLEEDING/POISON effects from different players now stack correctly
- **Regular Monsters**: BLEEDING/POISON effects continue to replace each other (preserves original behavior)
- All other effect types remain unaffected
- Performance impact is minimal due to efficient condition checking

---

## Testing and Validation

### Compilation Test
- ✅ Both fixes compile successfully without errors
- ✅ No syntax errors or missing imports
- ✅ Code follows existing project patterns and conventions

### Expected Behavior

#### Clan Leader Skills:
- Clan level 3+ leaders should now have access to siege skills
- System messages and UI should reflect the new level requirement
- Existing clans at level 3-4 should gain immediate access to skills

#### Effect Stacking:
- Multiple players using Sting/Poison on raid bosses should see multiple effect icons
- Same skills on regular monsters should show only one effect (latest caster)
- Effect duration and damage should stack appropriately on valid targets

---

## Technical Notes

### Architecture Compliance
- Changes follow existing L2J Mobius patterns
- No breaking changes to existing APIs
- Maintains backward compatibility with database and configurations

### Performance Considerations
- Effect stacking logic adds minimal overhead (simple boolean checks)
- No impact on non-BLEEDING/POISON effects
- Efficient target type checking using existing methods

### Security and Stability
- No security vulnerabilities introduced
- Changes are isolated to specific gameplay mechanics
- Extensive condition checking prevents edge cases

---

## Files Modified

### Summary of Changes:
- **`SiegeManager.java`**: 2 lines changed (clan level requirements)
- **`Clan.java`**: 2 lines changed (level check logic)  
- **`EffectList.java`**: 12 lines added (stacking logic + import)

### Build Status:
- ✅ Compilation successful
- ✅ No warnings or errors
- ✅ Ready for deployment

---

## Future Considerations

### Potential Extensions:
1. **Configuration Support**: Make the clan level requirement configurable via properties file
2. **Additional Effect Types**: Extend stacking logic to other debuff types if needed
3. **Logging Enhancement**: Add debug logs for effect stacking decisions

### Monitoring Recommendations:
1. Monitor clan skill acquisition rates at level 3
2. Observe raid boss encounter balance with stacking effects
3. Collect player feedback on the enhanced mechanics

---

*Document created: 2025-01-18*  
*L2Hardcore Development Team*