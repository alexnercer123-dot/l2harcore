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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.AutobotManager;
import org.l2jmobius.gameserver.model.actor.Autobot;

/**
 * Task manager for autobot AI processing
 */
public class AutobotTaskManager
{
    private static final int AI_TASK_DELAY = 500; // 0.5 seconds for more responsive AI
    private ScheduledFuture<?> _aiTask;
    private boolean _isActive = false;
    
    protected AutobotTaskManager()
    {
        // Initialize task manager
    }
    
    /**
     * Start the autobot AI processing
     */
    public void startAIProcessing()
    {
        if (_isActive)
        {
            return;
        }
        
        _isActive = true;
        _aiTask = ThreadPool.scheduleAtFixedRate(this::processAutobotAI, AI_TASK_DELAY, AI_TASK_DELAY);
    }
    
    /**
     * Stop the autobot AI processing
     */
    public void stopAIProcessing()
    {
        if (!_isActive)
        {
            return;
        }
        
        _isActive = false;
        if (_aiTask != null)
        {
            _aiTask.cancel(false);
            _aiTask = null;
        }
    }
    
    /**
     * Process AI for all active autobots
     */
    private void processAutobotAI()
    {
        try
        {
            Collection<Autobot> autobots = AutobotManager.getInstance().getActiveAutobots();
            
            if (autobots.isEmpty())
            {
                return; // No autobots to process
            }
            
            // Process advanced AI coordination
            org.l2jmobius.gameserver.ai.AdvancedAI.processCoordination();
            
            // Process individual autobot AI
            for (Autobot autobot : autobots)
            {
                if (autobot != null && autobot.isOnline() && autobot.getPlayer() != null)
                {
                    // Ensure AI is active and force it to think
                    if (autobot.getPlayer().getAI() != null)
                    {
                        // Force the AI to think by setting intention
                        autobot.getPlayer().getAI().setIntention(org.l2jmobius.gameserver.ai.Intention.ACTIVE);
                        
                        // Directly call the AI's onEvtThink method if it's our AutobotAI
                        if (autobot.getPlayer().getAI() instanceof org.l2jmobius.gameserver.ai.AutobotAI)
                        {
                            ((org.l2jmobius.gameserver.ai.AutobotAI) autobot.getPlayer().getAI()).onEvtThink();
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            // Log error but don't stop the task
            System.err.println("Error processing autobot AI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if AI processing is active
     */
    public boolean isActive()
    {
        return _isActive;
    }
    
    /**
     * Get current active autobots count being processed
     */
    public int getProcessedAutobotsCount()
    {
        return AutobotManager.getInstance().getActiveAutobotsCount();
    }
    
    /**
     * Get singleton instance
     */
    public static AutobotTaskManager getInstance()
    {
        return SingletonHolder.INSTANCE;
    }
    
    private static class SingletonHolder
    {
        protected static final AutobotTaskManager INSTANCE = new AutobotTaskManager();
    }
}