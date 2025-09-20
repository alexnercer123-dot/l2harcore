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
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Calendar;

import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;

/**
 * GameTime task manager class.
 * @author Forsaiken, Mobius
 */
public class GameTimeTaskManager extends Thread
{
	public static final int TICKS_PER_SECOND = 10; // Not able to change this without checking through code.
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	
	private final long _referenceTime;
	private long _timeOffset = 0; // Custom time offset for admin commands
	private boolean _isNight;
	private int _gameTicks;
	private int _gameTime;
	private int _gameHour;
	
	protected GameTimeTaskManager()
	{
		super("GameTimeTaskManager");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		
		super.start();
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			_gameTicks = (int) ((System.currentTimeMillis() - _referenceTime + _timeOffset) / MILLIS_IN_TICK);
			_gameTime = (_gameTicks % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
			_gameHour = _gameTime / 60;
			
			if ((_gameHour < 6) != _isNight)
			{
				_isNight = !_isNight;
				
				if (EventDispatcher.getInstance().hasListener(EventType.ON_DAY_NIGHT_CHANGE))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnDayNightChange(_isNight));
				}
			}
			
			try
			{
				Thread.sleep(MILLIS_IN_TICK);
			}
			catch (InterruptedException e)
			{
				// Ignore.
			}
		}
	}
	
	public boolean isNight()
	{
		return _isNight;
	}
	
	/**
	 * @return The actual GameTime tick. Directly taken from current time.
	 */
	public int getGameTicks()
	{
		return _gameTicks;
	}
	
	public int getGameTime()
	{
		return _gameTime;
	}
	
	public int getGameHour()
	{
		return _gameHour;
	}
	
	public int getGameMinute()
	{
		return _gameTime % 60;
	}
	
	/**
	 * Sets the game time to a specific minute of the in-game day.
	 * @param gameTimeInMinutes the time in minutes (0-1439, where 1440 minutes = 1 in-game day)
	 */
	public void setGameTimeInMinutes(int gameTimeInMinutes)
	{
		// Ensure the time is within valid range (0-1439 minutes per in-game day)
		final int timeInDay = gameTimeInMinutes % 1440;
		
		// Calculate the current game time without offset
		final int currentGameTicks = (int) ((System.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
		final int currentGameTime = (currentGameTicks % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
		
		// Calculate the required offset to reach the target time
		final int targetGameTime = timeInDay;
		final int timeDifference = targetGameTime - currentGameTime;
		
		// Convert time difference to milliseconds and apply as offset
		_timeOffset += (long) timeDifference * MILLIS_IN_TICK;
		
		// Immediately update current values
		_gameTicks = (int) ((System.currentTimeMillis() - _referenceTime + _timeOffset) / MILLIS_IN_TICK);
		_gameTime = (_gameTicks % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
		_gameHour = _gameTime / 60;
		
		// Check for day/night change
		final boolean wasNight = _isNight;
		_isNight = _gameHour < 6;
		
		// Trigger day/night change event if state changed
		if (wasNight != _isNight)
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_DAY_NIGHT_CHANGE))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnDayNightChange(_isNight));
			}
		}
	}
	
	/**
	 * Sets the game time to a specific hour (0-23).
	 * @param hour the hour to set (0-23)
	 */
	public void setGameTime(int hour)
	{
		// Convert hour to minutes (each hour = 60 minutes)
		final int gameTimeInMinutes = (hour % 24) * 60;
		setGameTimeInMinutes(gameTimeInMinutes);
	}
	
	public static final GameTimeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameTimeTaskManager INSTANCE = new GameTimeTaskManager();
	}
}
