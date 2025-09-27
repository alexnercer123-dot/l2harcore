/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */
package org.l2jmobius.gameserver.ai;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Autobot;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.commons.util.Rnd;

import static java.util.stream.Collectors.toList;

/**
 * Enhanced AI for autobot with state management and intelligent behavior
 */
public class AutobotAI extends PlayerAI
{
    private static final Logger FAKE_PLAYER_LOGGER = AutobotManager.getFakePlayerLogger();
    
    // AI States
    public enum AIState
    {
        IDLE,
        EXPLORING,
        COMBAT,
        FLEEING,
        SOCIALIZING,
        RETURNING_HOME,
        FARMING
    }
    
    private final Autobot _autobot;
    private AIState _currentState = AIState.IDLE;
    private long _lastStateChange = System.currentTimeMillis();
    private long _lastActionTime = System.currentTimeMillis();
    private Location _lastKnownPosition;
    private int _stuckCounter = 0;
    private int _aggressionLevel = 50; // 0-100 scale
    private int _socialLevel = 30; // 0-100 scale
    private boolean _isGroupHunter = false;
    
    // Combat related
    private Creature _lastTarget;
    private long _lastCombatTime = 0;
    private int _combatWins = 0;
    private int _combatLosses = 0;
    
    // Movement related
    private Location _currentDestination;
    private long _destinationSetTime = 0;
    private static final long DESTINATION_TIMEOUT = 30000; // 30 seconds
    
    // Logging related
    private long _lastMobScanTime = 0;
    private long _lastThinkTime = 0; // To prevent command spamming
    
    // Enhanced target switching - track checked targets to avoid returning to previous ones
    private Set<Integer> _checkedTargets = new HashSet<>();
    private long _lastTargetSwitchTime = 0;
    
    public AutobotAI(Autobot autobot)
    {
        super(autobot.getPlayer());
        _autobot = autobot;
        initializePersonality();
        _lastKnownPosition = autobot.getHomeLocation();
        
        // Ensure AI starts thinking immediately
        FAKE_PLAYER_LOGGER.info("AutobotAI initialized for: " + autobot.getName());
    }
    
    /**
     * Initialize bot personality traits
     */
    private void initializePersonality()
    {
        _aggressionLevel = ThreadLocalRandom.current().nextInt(20, 80);
        _socialLevel = ThreadLocalRandom.current().nextInt(10, 60);
        _isGroupHunter = Rnd.nextBoolean();
    }
    
    /**
     * Get current AI state for debugging
     */
    public AIState getCurrentState()
    {
        return _currentState;
    }
    
    /**
     * Get aggression level
     */
    public int getAggressionLevel()
    {
        return _aggressionLevel;
    }
    
    /**
     * Get social level
     */
    public int getSocialLevel()
    {
        return _socialLevel;
    }
    
    public void onEvtThink()
    {
        if (!_autobot.isOnline() || _autobot.getPlayer() == null)
        {
            return;
        }
        
        // Check for death and handle despawning
        _autobot.checkDeathStatus();
        if (!_autobot.isOnline()) // If autobot was despawned due to death, stop thinking
        {
            return;
        }
        
        // Reduce thinking frequency to prevent movement interruption and command spamming
        long currentTime = System.currentTimeMillis();
        
        // Use different thinking intervals based on current activity
        long thinkingInterval;
        if (_autobot.getPlayer().isMoving())
        {
            // When moving, think much less frequently to avoid interrupting movement
            thinkingInterval = 5000; // 5 seconds when moving (increased from 3)
        }
        else if (_autobot.getPlayer().isInCombat() || _autobot.getPlayer().isCastingNow() || _autobot.getPlayer().isAttackingNow())
        {
            // When in combat, think quickly for responsiveness
            thinkingInterval = 800; // 0.8 seconds in combat (decreased from 1.5)
        }
        else
        {
            // Normal thinking interval when idle
            thinkingInterval = 2500; // 2.5 seconds when idle (increased from 2)
        }
        
        if (currentTime - _lastThinkTime < thinkingInterval)
        {
            return;
        }
        _lastThinkTime = currentTime;
        
        // Add periodic logging to see if AI is thinking
        if (currentTime - _lastActionTime > 30000) // Every 30 seconds
        {
            FAKE_PLAYER_LOGGER.info(_autobot.getPlayer().getName() + " AI thinking - State: " + _currentState + 
                ", Location: " + _autobot.getPlayer().getLocation() + ", Moving: " + _autobot.getPlayer().isMoving());
            _lastActionTime = currentTime;
        }
        
        // Check if current target is nearly dead - prioritize finishing it
        if (_currentState == AIState.COMBAT && _autobot.getPlayer().getTarget() instanceof Creature)
        {
            Creature currentTarget = (Creature) _autobot.getPlayer().getTarget();
            // Ensure we're only targeting monsters, never players
            if (currentTarget != null && !currentTarget.isDead() && !(currentTarget instanceof Player))
            {
                double hpPercent = (currentTarget.getCurrentHp() / currentTarget.getMaxHp()) * 100;
                if (hpPercent < 25) // Target has less than 25% HP
                {
                    FAKE_PLAYER_LOGGER.info(_autobot.getPlayer().getName() + " prioritizing nearly dead target " + 
                        currentTarget.getName() + " (" + String.format("%.1f", hpPercent) + "% HP)");
                    // Skip state update and continue combat to finish this target
                    executeStateAction();
                    return;
                }
            }
            else if (currentTarget instanceof Player)
            {
                // If somehow targeting a player, clear target immediately
                FAKE_PLAYER_LOGGER.warning(_autobot.getPlayer().getName() + " was targeting a player, clearing target!");
                _autobot.getPlayer().setTarget(null);
                _lastTarget = null;
            }
        }
        
        // Update AI state
        updateAIState();
        
        // Execute state action
        executeStateAction();
        
        // Check if stuck
        checkIfStuck();
        
        // Update last action time if moving or fighting
        if (_autobot.getPlayer().isMoving() || _autobot.getPlayer().isInCombat())
        {
            _lastActionTime = currentTime;
        }
        
        // Call superclass think method to ensure proper AI behavior
        // super.onEvtThink();
    }
    
    /**
     * Update AI state based on current conditions
     */
    private void updateAIState()
    {
        AIState newState = _currentState;
        Player player = _autobot.getPlayer();
        
        // Check for combat first
        if (player.getTarget() != null && player.getTarget() instanceof Creature)
        {
            Creature target = (Creature) player.getTarget();
            if (!target.isDead() && target.isAttackable())
            {
                newState = AIState.COMBAT;
            }
        }
        // Check if need to flee
        else if (shouldFlee())
        {
            newState = AIState.FLEEING;
        }
        // Check if too far from home
        else if (isToFarFromHome())
        {
            newState = AIState.RETURNING_HOME;
        }
        // Prioritize farming for leveling - increased frequency
        else if (_autobot.isAutoFarmEnabled() && shouldFarm())
        {
            newState = AIState.FARMING;
        }
        // Check for social interaction
        else if (shouldSocialize())
        {
            newState = AIState.SOCIALIZING;
        }
        // Random exploration
        else if (shouldExplore())
        {
            newState = AIState.EXPLORING;
        }
        // Default to farming if not in peace zone and no other activity
        else if (!player.isInsideZone(ZoneId.PEACE))
        {
            newState = AIState.FARMING;
        }
        else
        {
            newState = AIState.IDLE;
        }
        
        if (newState != _currentState)
        {
            FAKE_PLAYER_LOGGER.info(_autobot.getPlayer().getName() + " state change: " + _currentState + " -> " + newState);
            _currentState = newState;
            _lastStateChange = System.currentTimeMillis();
        }
    }
    
    /**
     * Execute action based on current state
     */
    private void executeStateAction()
    {
        switch (_currentState)
        {
            case COMBAT:
                executeCombatAction();
                break;
            case EXPLORING:
                executeExplorationAction();
                break;
            case FLEEING:
                executeFleeAction();
                break;
            case SOCIALIZING:
                executeSocialAction();
                break;
            case RETURNING_HOME:
                executeReturnHomeAction();
                break;
            case FARMING:
                executeFarmingAction();
                break;
            case IDLE:
            default:
                executeIdleAction();
                break;
        }
    }
    
    /**
     * Execute combat action using enhanced CombatAI with target persistence
     */
    private void executeCombatAction()
    {
        Player player = _autobot.getPlayer();
        if (player.getTarget() != null && player.getTarget() instanceof Creature)
        {
            Creature target = (Creature) player.getTarget();
            
            // CRITICAL: Ensure we never attack players - only monsters
            if (target instanceof Player)
            {
                FAKE_PLAYER_LOGGER.warning(player.getName() + " was targeting player " + target.getName() + ", clearing target!");
                player.setTarget(null);
                _lastTarget = null;
                _currentState = AIState.FARMING;
                return;
            }
            
            // Check if target is still valid and alive
            if (target.isDead() || !target.isAttackable())
            {
                if (target.isDead())
                {
                    FAKE_PLAYER_LOGGER.info(player.getName() + " killed " + target.getName() + " (" + target.getId() + ")");
                    // Target is dead, we can look for a new one
                    _lastTarget = null;
                    player.setTarget(null);
                    
                    // Immediately try to find another target to continue fighting
                    if (!findAndAttackTarget())
                    {
                        _currentState = AIState.FARMING;
                    }
                }
                else
                {
                    // Target became invalid but not dead, clear and find new one
                    _lastTarget = null;
                    player.setTarget(null);
                }
                return;
            }
            
            // IMPORTANT: Don't abandon target if already engaged in combat
            // Only check for target conflicts if we haven't started attacking yet
            if (_lastTarget != target && isTargetBeingAttacked(target))
            {
                FAKE_PLAYER_LOGGER.info(player.getName() + " abandoning " + target.getName() + " (already being attacked), looking for new target");
                
                // Mark current target as checked to avoid returning to it
                _checkedTargets.add(target.getObjectId());
                player.setTarget(null);
                _lastTarget = null;
                
                // Try to find a new target immediately
                if (!findAndAttackTarget())
                {
                    _currentState = AIState.FARMING;
                }
                return;
            }
            
            // Log combat engagement (once per target)
            if (_lastTarget != target)
            {
                FAKE_PLAYER_LOGGER.info(player.getName() + " engaging in combat with " + target.getName() + 
                    " (" + target.getId() + ") at " + target.getLocation() + 
                    " HP: " + target.getCurrentHp() + "/" + target.getMaxHp());
                _lastTarget = target;
            }
            
            // Use enhanced combat AI with spam prevention - IMMEDIATE RESPONSE
            if (CombatAI.executeCombat(_autobot, target))
            {
                _lastCombatTime = System.currentTimeMillis();
            }
        }
        else
        {
            // No target, try to find one
            if (!findAndAttackTarget())
            {
                // No targets found, switch to farming state to keep looking
                _currentState = AIState.FARMING;
            }
        }
    }
    
    /**
     * Execute exploration action using DynamicPathfinder with improved movement continuity
     */
    private void executeExplorationAction()
    {
        // Only generate new destination if we don't have one, reached it, or it's timed out
        if (_currentDestination == null || hasReachedDestination() || 
            (System.currentTimeMillis() - _destinationSetTime) > DESTINATION_TIMEOUT)
        {
            generateExplorationDestination();
            if (_currentDestination != null)
            {
                FAKE_PLAYER_LOGGER.fine(_autobot.getPlayer().getName() + " exploring to " + _currentDestination);
            }
        }
        
        // Only issue movement command if we're not already moving and have a destination
        if (_currentDestination != null && !_autobot.getPlayer().isMoving())
        {
            // Simple direct movement without pathfinding to reduce calculations
            moveTo(_currentDestination);
        }
        // If already moving, let the movement continue without any interruption
    }
    
    /**
     * Execute flee action with movement continuity
     */
    private void executeFleeAction()
    {
        // Only initiate flee movement if not already moving
        if (!_autobot.getPlayer().isMoving())
        {
            Location safeLocation = findSafeLocation();
            if (safeLocation != null)
            {
                moveTo(safeLocation);
            }
            else
            {
                // Fallback: move to home
                executeReturnHomeAction();
            }
        }
    }
    
    /**
     * Execute social action - DISABLED
     */
    private void executeSocialAction()
    {
        // Social interactions disabled to prevent party formation and chat
        // Switch back to farming or exploration instead
        _currentState = AIState.FARMING;
    }
    
    /**
     * Execute return home action with movement continuity
     */
    private void executeReturnHomeAction()
    {
        Location homeLocation = _autobot.getHomeLocation();
        if (homeLocation != null && !_autobot.getPlayer().isMoving())
        {
            moveTo(homeLocation);
        }
    }
    
    /**
     * Execute farming action
     */
    private void executeFarmingAction()
    {
        // Log what mobs the bot can see around itself
        logNearbyMobs();
        
        if (!findAndAttackTarget())
        {
            // No targets found, explore farming area
            executeExplorationAction();
        }
    }
    
    /**
     * Execute idle action with more aggressive target seeking
     */
    private void executeIdleAction()
    {
        // More frequently try to find targets when idle
        if (!_autobot.getPlayer().isInsideZone(ZoneId.PEACE))
        {
            // Try to find targets when idle (outside of peace zones)
            if (findAndAttackTarget())
            {
                _currentState = AIState.COMBAT; // Immediately switch to combat state
                return; // Found target, combat will handle it
            }
            
            // No targets found, switch to farming to keep looking more aggressively
            _currentState = AIState.FARMING;
            return;
        }
        
        // Occasionally do random actions when idle (reduced frequency)
        if (Rnd.get(100) < 2) // Reduced from 5% to 2% to focus more on combat
        {
            int action = Rnd.get(4);
            switch (action)
            {
                case 0:
                    _currentState = AIState.EXPLORING;
                    break;
                case 1:
                    if (_autobot.getPlayer().isInsideZone(ZoneId.PEACE))
                    {
                        _currentState = AIState.SOCIALIZING;
                    }
                    else
                    {
                        _currentState = AIState.FARMING; // Prefer farming over socializing outside peace zones
                    }
                    break;
                case 2:
                    // Sit down for a moment
                    if (!_autobot.getPlayer().isSitting())
                    {
                        _autobot.getPlayer().sitDown();
                    }
                    break;
                case 3:
                    // Stand up if sitting
                    if (_autobot.getPlayer().isSitting())
                    {
                        _autobot.getPlayer().standUp();
                    }
                    break;
            }
        }
    }
    
    /**
     * Find and attack suitable target with enhanced target switching logic and priority system
     */
    private boolean findAndAttackTarget()
    {
        Player player = _autobot.getPlayer();
        
        // Clear checked targets every 45 seconds to allow revisiting (increased from 30)
        long currentTime = System.currentTimeMillis();
        if (currentTime - _lastTargetSwitchTime > 45000)
        {
            _checkedTargets.clear();
            _lastTargetSwitchTime = currentTime;
            FAKE_PLAYER_LOGGER.info(player.getName() + " cleared checked targets list, can revisit previous mobs");
        }
        
        // First, try to find Gremlins nearby (only target Gremlins) with 1000 radius
        List<Creature> nearbyGremlins = World.getInstance().getVisibleObjectsInRange(player, Attackable.class, 1000)
            .stream()
            .filter(mob -> mob instanceof Attackable && !mob.isDead() && mob.isAttackable())
            .filter(mob -> mob.getName().toLowerCase().contains("gremlin")) // Only target Gremlins
            .filter(mob -> !_checkedTargets.contains(mob.getObjectId())) // Not previously checked in this cycle
            .map(mob -> (Creature) mob)
            .collect(toList());
        
        if (nearbyGremlins.isEmpty())
        {
            FAKE_PLAYER_LOGGER.info(player.getName() + " found no available Gremlins within 1000 range");
            return false;
        }
        
        // Prioritize targets: wounded targets first, then closest targets
        Creature bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        
        for (Creature gremlin : nearbyGremlins)
        {
            // Skip if someone else is already attacking (but be less restrictive)
            int attackerCount = countAttackersOnTarget(gremlin);
            if (attackerCount >= 2) // Allow up to 2 attackers per target instead of 1
            {
                continue;
            }
            
            double distance = player.calculateDistance2D(gremlin);
            double hpPercent = (gremlin.getCurrentHp() / gremlin.getMaxHp()) * 100;
            
            // Calculate priority score (lower is better)
            double score = distance;
            
            // Heavily prioritize wounded targets (reduce score for low HP targets)
            if (hpPercent < 50)
            {
                score *= 0.3; // Wounded targets get 70% priority boost
                FAKE_PLAYER_LOGGER.info(player.getName() + " found wounded Gremlin " + gremlin.getName() + 
                    " with " + String.format("%.1f", hpPercent) + "% HP - prioritizing");
            }
            else if (hpPercent < 75)
            {
                score *= 0.6; // Partially wounded targets get 40% priority boost
            }
            
            // Prioritize targets that no one else is attacking
            if (attackerCount == 0)
            {
                score *= 0.8; // 20% bonus for uncontested targets
            }
            
            if (score < bestScore)
            {
                bestScore = score;
                bestTarget = gremlin;
            }
        }
        
        if (bestTarget != null)
        {
            // Mark this target as checked to avoid returning to it immediately
            _checkedTargets.add(bestTarget.getObjectId());
            
            double targetHpPercent = (bestTarget.getCurrentHp() / bestTarget.getMaxHp()) * 100;
            
            // Log target selection with more details
            FAKE_PLAYER_LOGGER.info(player.getName() + " targeting Gremlin " + bestTarget.getName() + 
                " (" + bestTarget.getId() + ") at distance " + String.format("%.1f", player.calculateDistance2D(bestTarget)) +
                ", HP: " + String.format("%.1f", targetHpPercent) + "%, attackers: " + countAttackersOnTarget(bestTarget));
            
            // Set target and use proper attack command with enhanced spam prevention
            player.setTarget(bestTarget);
            
            // Enhanced spam prevention - check if already performing any action
            if (!player.isAttackingNow() && !player.isCastingNow() && !player.isMoving() &&
                player.getAI().getIntention() != Intention.ATTACK &&
                player.getAI().getIntention() != Intention.CAST)
            {
                player.getAI().setIntention(Intention.ATTACK, bestTarget);
            }
            return true;
        }
        
        // No suitable Gremlins found
        FAKE_PLAYER_LOGGER.info(player.getName() + " found " + nearbyGremlins.size() + " Gremlins but all are being attacked by 2+ players");
        return false;
    }
    
    /**
     * Check if target is already being attacked by another player using original RuAcis logic
     */
    private boolean isTargetBeingAttacked(Creature target)
    {
        if (target == null)
        {
            return false;
        }
        
        return countAttackersOnTarget(target) >= 2; // Allow up to 2 attackers per target
    }
    
    /**
     * Count how many players are currently attacking this target
     */
    private int countAttackersOnTarget(Creature target)
    {
        if (target == null)
        {
            return 0;
        }
        
        int attackerCount = 0;
        
        for (Player other : World.getInstance().getPlayers())
        {
            if (other == null || other == _autobot.getPlayer() || other.getTarget() == null)
                continue;
            
            if (other.getTarget() == target)
            {
                // Count any player targeting this mob
                attackerCount++;
            }
        }
        
        return attackerCount;
    }
    
    /**
     * Check if two autobots are in the same party or clan (original RuAcis logic)
     */
    private static boolean isSamePartyOrClan(Autobot player1, Autobot player2)
    {
        if (player1 == null || player2 == null)
            return false;
        
        if (player1.getPlayer().getParty() != null && player1.getPlayer().getParty().equals(player2.getPlayer().getParty()))
            return true;
        
        if (player1.getPlayer().getClan() != null && player1.getPlayer().getClan().equals(player2.getPlayer().getClan()))
            return true;
        
        return false;
    }
    
    /**
     * Log nearby mobs that the bot can detect
     */
    private void logNearbyMobs()
    {
        Player player = _autobot.getPlayer();
        
        // Check every 10 seconds to avoid spam
        if (System.currentTimeMillis() - _lastMobScanTime < 10000)
        {
            return;
        }
        _lastMobScanTime = System.currentTimeMillis();
        
        // Get all nearby creatures in a wider range, focusing on Gremlins
        List<Creature> nearbyCreatures = World.getInstance().getVisibleObjectsInRange(player, Creature.class, 1000)
            .stream()
            .filter(creature -> !(creature instanceof Player)) // Exclude other players - only target monsters
            .collect(toList());
        
        // Separate Gremlins from other creatures
        List<Creature> gremlins = nearbyCreatures.stream()
            .filter(creature -> creature.getName().toLowerCase().contains("gremlin"))
            .collect(toList());
        
        if (nearbyCreatures.isEmpty())
        {
            FAKE_PLAYER_LOGGER.info(player.getName() + " at " + player.getLocation() + " - No mobs detected within 1000 range");
            return;
        }
        
        // Log summary of nearby mobs with special focus on Gremlins
        StringBuilder mobInfo = new StringBuilder();
        mobInfo.append(player.getName()).append(" at ").append(player.getLocation())
            .append(" detects ").append(nearbyCreatures.size()).append(" creatures")
            .append(" (including ").append(gremlins.size()).append(" Gremlins):\n");
        
        // Group mobs by type and count them
        java.util.Map<String, java.util.List<Creature>> mobGroups = nearbyCreatures.stream()
            .collect(java.util.stream.Collectors.groupingBy(creature -> creature.getName()));
        
        for (java.util.Map.Entry<String, java.util.List<Creature>> entry : mobGroups.entrySet())
        {
            String mobName = entry.getKey();
            java.util.List<Creature> mobs = entry.getValue();
            
            // Find closest and furthest of this type
            double minDistance = mobs.stream()
                .mapToDouble(mob -> player.calculateDistance2D(mob))
                .min().orElse(0);
            double maxDistance = mobs.stream()
                .mapToDouble(mob -> player.calculateDistance2D(mob))
                .max().orElse(0);
            
            // Check how many are attackable
            long attackableCount = mobs.stream()
                .filter(mob -> mob instanceof Attackable && !mob.isDead() && mob.isAttackable())
                .count();
            
            mobInfo.append("  - ").append(mobName).append(": ").append(mobs.size()).append(" total")
                .append(", ").append(attackableCount).append(" attackable")
                .append(", range ").append(String.format("%.1f", minDistance))
                .append("-").append(String.format("%.1f", maxDistance)).append("\n");
        }
        
        FAKE_PLAYER_LOGGER.info(mobInfo.toString());
    }
    
    /**
     * Check if target is valid for attacking (using SmartTargetSelector)
     */
    private boolean isValidTarget(Creature target)
    {
        return SmartTargetSelector.isValidTarget(target, _autobot);
    }
    
    /**
     * Calculate target priority score (deprecated - using SmartTargetSelector)
     */
    @Deprecated
    private double calculateTargetScore(Creature target)
    {
        // This method is deprecated in favor of SmartTargetSelector
        return 50;
    }
    
    // Helper methods
    private boolean shouldFlee()
    {
        Player player = _autobot.getPlayer();
        double hpPercent = (player.getCurrentHp() / player.getMaxHp()) * 100;
        
        // Flee if low HP and low aggression
        if (hpPercent < 30 && _aggressionLevel < 40)
        {
            return true;
        }
        
        // Flee if overwhelmed by enemies
        long enemyCount = World.getInstance().getVisibleObjectsInRange(player, Player.class, 200)
            .stream()
            .filter(p -> p.getTarget() == player)
            .count();
            
        return enemyCount > 2 && _aggressionLevel < 60;
    }
    
    private boolean isToFarFromHome()
    {
        Location homeLocation = _autobot.getHomeLocation();
        if (homeLocation == null)
        {
            return false;
        }
        
        double distance = _autobot.getPlayer().calculateDistance2D(homeLocation);
        return distance > 1500; // Allow roaming with 1000 search radius
    }
    
    private boolean shouldSocialize()
    {
        // Disable socializing to prevent party formation and focus on hunting
        return false;
    }
    
    private boolean shouldExplore()
    {
        if (Rnd.get(100) < (_aggressionLevel + _socialLevel) / 2)
        {
            long timeSinceStateChange = System.currentTimeMillis() - _lastStateChange;
            return timeSinceStateChange > 30000; // 30 seconds in same state
        }
        return false;
    }
    
    private boolean shouldFarm()
    {
        // Always farm when outside peace zones and auto farm is enabled
        return _autobot.isAutoFarmEnabled() && !_autobot.getPlayer().isInsideZone(ZoneId.PEACE);
    }
    
    /**
     * Generate exploration destination using DynamicPathfinder patterns
     */
    private void generateExplorationDestination()
    {
        Player player = _autobot.getPlayer();
        Location currentLoc = player.getLocation();
        
        // Choose movement pattern based on personality
        DynamicPathfinder.MovementPattern pattern = DynamicPathfinder.MovementPattern.RANDOM;
        
        if (_aggressionLevel > 60)
        {
            pattern = DynamicPathfinder.MovementPattern.DIRECT;
        }
        else if (_socialLevel > 50)
        {
            pattern = DynamicPathfinder.MovementPattern.CIRCULAR;
        }
        else if (_isGroupHunter)
        {
            pattern = DynamicPathfinder.MovementPattern.PATROL;
        }
        
        // Generate pattern path
        List<Location> patternPath = DynamicPathfinder.generatePatternPath(
            currentLoc, pattern, ThreadLocalRandom.current().nextInt(300, 800), 5);
        
        if (!patternPath.isEmpty())
        {
            // Pick a random point from the pattern
            _currentDestination = patternPath.get(ThreadLocalRandom.current().nextInt(patternPath.size()));
            _destinationSetTime = System.currentTimeMillis();
        }
        else
        {
            // Fallback to simple random destination
            generateSimpleDestination(currentLoc);
        }
    }
    
    /**
     * Fallback method for simple destination generation
     */
    private void generateSimpleDestination(Location currentLoc)
    {
        for (int i = 0; i < 10; i++) // Try 10 times to find valid location
        {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            int distance = ThreadLocalRandom.current().nextInt(300, 800);
            
            int x = currentLoc.getX() + (int)(Math.cos(angle) * distance);
            int y = currentLoc.getY() + (int)(Math.sin(angle) * distance);
            int z = GeoEngine.getInstance().getHeight(x, y, currentLoc.getZ());
            
            Location newLoc = new Location(x, y, z);
            
            if (GeoEngine.getInstance().canMoveToTarget(currentLoc, newLoc))
            {
                _currentDestination = newLoc;
                _destinationSetTime = System.currentTimeMillis();
                break;
            }
        }
    }
    
    private Location findSafeLocation()
    {
        Player player = _autobot.getPlayer();
        Location currentLoc = player.getLocation();
        
        // Try to find a location away from enemies
        for (int i = 0; i < 8; i++)
        {
            double angle = (i * Math.PI / 4); // 8 directions
            int distance = 500;
            
            int x = currentLoc.getX() + (int)(Math.cos(angle) * distance);
            int y = currentLoc.getY() + (int)(Math.sin(angle) * distance);
            int z = GeoEngine.getInstance().getHeight(x, y, currentLoc.getZ());
            
            Location safeLoc = new Location(x, y, z);
            
            if (GeoEngine.getInstance().canMoveToTarget(currentLoc, safeLoc))
            {
                return safeLoc;
            }
        }
        
        return _autobot.getHomeLocation(); // Fallback to home
    }
    
    private Location calculateApproachLocation(Location target, int distance)
    {
        Player player = _autobot.getPlayer();
        Location currentLoc = player.getLocation();
        
        double angle = Math.atan2(target.getY() - currentLoc.getY(), target.getX() - currentLoc.getX());
        
        int x = target.getX() - (int)(Math.cos(angle) * distance);
        int y = target.getY() - (int)(Math.sin(angle) * distance);
        int z = GeoEngine.getInstance().getHeight(x, y, target.getZ());
        
        Location approachLoc = new Location(x, y, z);
        
        return GeoEngine.getInstance().canMoveToTarget(currentLoc, approachLoc) ? approachLoc : null;
    }
    
    private void moveTo(Location destination)
    {
        if (destination != null)
        {
            Player player = _autobot.getPlayer();
            
            // Only issue movement command if we're not already moving or destination is far
            if (!player.isMoving() || player.calculateDistance2D(destination) > 200)
            {
                player.getAI().setIntention(Intention.MOVE_TO, destination);
                FAKE_PLAYER_LOGGER.fine(player.getName() + " moving to " + destination);
            }
        }
    }
    
    private boolean hasReachedDestination()
    {
        if (_currentDestination == null)
        {
            return true;
        }
        
        return _autobot.getPlayer().calculateDistance2D(_currentDestination) < 100;
    }
    
    private void checkIfStuck()
    {
        Player player = _autobot.getPlayer();
        Location currentPos = player.getLocation();
        
        if (_lastKnownPosition != null && calculateDistance(currentPos, _lastKnownPosition) < 50)
        {
            _stuckCounter++;
            
            if (_stuckCounter >= 5) // Stuck for 5 checks
            {
                handleStuckSituation();
                _stuckCounter = 0;
            }
        }
        else
        {
            _stuckCounter = 0;
        }
        
        _lastKnownPosition = currentPos;
    }
    
    private void handleStuckSituation()
    {
        // Try to find alternative path or teleport
        generateExplorationDestination();
        
        if (_currentDestination == null)
        {
            // Last resort: move to home
            executeReturnHomeAction();
        }
    }
    
    /**
     * Calculate distance between two locations
     */
    private double calculateDistance(Location loc1, Location loc2)
    {
        if (loc1 == null || loc2 == null)
        {
            return Double.MAX_VALUE;
        }
        
        double dx = loc1.getX() - loc2.getX();
        double dy = loc1.getY() - loc2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}