/*
 * Copyright (C) 2004-2022 L2J Mobius
 * 
 * This file is part of L2J Mobius.
 * 
 * L2J Mobius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Mobius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.commons.util.Rnd;

/**
 * Advanced pathfinding system for autobots with dynamic route calculation,
 * obstacle avoidance, and intelligent movement patterns.
 * @author YourName
 */
public class DynamicPathfinder
{
	private static final Logger LOGGER = Logger.getLogger(DynamicPathfinder.class.getName());
	
	// Movement patterns
	public enum MovementPattern
	{
		DIRECT,      // Direct line to target
		ZIGZAG,      // Zigzag pattern
		CIRCULAR,    // Circular movement
		RANDOM,      // Random walk
		PATROL,      // Patrol between points
		SPIRAL       // Spiral pattern
	}
	
	// Path node for A* algorithm
	private static class PathNode
	{
		Location location;
		PathNode parent;
		double gCost; // Distance from start
		double hCost; // Distance to end
		double fCost; // Total cost
		
		PathNode(Location loc)
		{
			location = loc;
		}
		
		void calculateFCost()
		{
			fCost = gCost + hCost;
		}
	}
	
	/**
	 * Calculate optimal path using A* algorithm with dynamic adjustments
	 */
	public static List<Location> calculatePath(Location start, Location end, Player player)
	{
		if (start == null || end == null || start.equals(end))
		{
			return Collections.emptyList();
		}
		
		// Check direct path first
		if (GeoEngine.getInstance().canMoveToTarget(start, end))
		{
			return List.of(start, end);
		}
		
		// Use A* for complex pathfinding
		return calculateAStarPath(start, end, player);
	}
	
	/**
	 * A* pathfinding implementation
	 */
	private static List<Location> calculateAStarPath(Location start, Location end, Player player)
	{
		List<PathNode> openList = new ArrayList<>();
		List<PathNode> closedList = new ArrayList<>();
		
		PathNode startNode = new PathNode(start);
		PathNode endNode = new PathNode(end);
		
		openList.add(startNode);
		
		int maxIterations = 100; // Prevent infinite loops
		int iterations = 0;
		
		while (!openList.isEmpty() && iterations < maxIterations)
		{
			iterations++;
			
			// Find node with lowest F cost
			PathNode currentNode = openList.get(0);
			for (PathNode node : openList)
			{
				if (node.fCost < currentNode.fCost)
				{
					currentNode = node;
				}
			}
			
			openList.remove(currentNode);
			closedList.add(currentNode);
			
			// Check if reached destination
			if (calculateDistance(currentNode.location, end) < 100)
			{
				return reconstructPath(currentNode);
			}
			
			// Check neighbor nodes
			List<Location> neighbors = getNeighborLocations(currentNode.location);
			for (Location neighborLoc : neighbors)
			{
				if (isInClosedList(neighborLoc, closedList))
				{
					continue;
				}
				
				if (!GeoEngine.getInstance().canMoveToTarget(currentNode.location, neighborLoc))
				{
					continue;
				}
				
				PathNode neighborNode = new PathNode(neighborLoc);
				neighborNode.parent = currentNode;
				neighborNode.gCost = currentNode.gCost + calculateDistance(currentNode.location, neighborLoc);
				neighborNode.hCost = calculateDistance(neighborLoc, end);
				neighborNode.calculateFCost();
				
				// Check if this path to neighbor is better
				PathNode existingNode = findInOpenList(neighborLoc, openList);
				if (existingNode == null)
				{
					openList.add(neighborNode);
				}
				else if (neighborNode.gCost < existingNode.gCost)
				{
					existingNode.parent = currentNode;
					existingNode.gCost = neighborNode.gCost;
					existingNode.calculateFCost();
				}
			}
		}
		
		// No path found, return direct line as fallback
		return List.of(start, end);
	}
	
	/**
	 * Generate movement path based on pattern
	 */
	public static List<Location> generatePatternPath(Location center, MovementPattern pattern, int radius, int steps)
	{
		List<Location> path = new ArrayList<>();
		
		switch (pattern)
		{
			case DIRECT:
				path.add(center);
				break;
				
			case ZIGZAG:
				generateZigzagPath(path, center, radius, steps);
				break;
				
			case CIRCULAR:
				generateCircularPath(path, center, radius, steps);
				break;
				
			case RANDOM:
				generateRandomPath(path, center, radius, steps);
				break;
				
			case PATROL:
				generatePatrolPath(path, center, radius, steps);
				break;
				
			case SPIRAL:
				generateSpiralPath(path, center, radius, steps);
				break;
		}
		
		return path;
	}
	
	/**
	 * Generate zigzag movement pattern
	 */
	private static void generateZigzagPath(List<Location> path, Location center, int radius, int steps)
	{
		for (int i = 0; i < steps; i++)
		{
			double angle = (i % 2 == 0) ? Math.PI / 4 : -Math.PI / 4; // 45 degrees alternating
			double distance = radius * (0.5 + (i * 0.1));
			
			int x = center.getX() + (int)(Math.cos(angle) * distance);
			int y = center.getY() + (int)(Math.sin(angle) * distance);
			int z = GeoEngine.getInstance().getHeight(x, y, center.getZ());
			
			path.add(new Location(x, y, z));
		}
	}
	
	/**
	 * Generate circular movement pattern
	 */
	private static void generateCircularPath(List<Location> path, Location center, int radius, int steps)
	{
		for (int i = 0; i < steps; i++)
		{
			double angle = (2 * Math.PI * i) / steps;
			
			int x = center.getX() + (int)(Math.cos(angle) * radius);
			int y = center.getY() + (int)(Math.sin(angle) * radius);
			int z = GeoEngine.getInstance().getHeight(x, y, center.getZ());
			
			path.add(new Location(x, y, z));
		}
	}
	
	/**
	 * Generate random movement pattern
	 */
	private static void generateRandomPath(List<Location> path, Location center, int radius, int steps)
	{
		for (int i = 0; i < steps; i++)
		{
			double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
			double distance = ThreadLocalRandom.current().nextDouble() * radius;
			
			int x = center.getX() + (int)(Math.cos(angle) * distance);
			int y = center.getY() + (int)(Math.sin(angle) * distance);
			int z = GeoEngine.getInstance().getHeight(x, y, center.getZ());
			
			path.add(new Location(x, y, z));
		}
	}
	
	/**
	 * Generate patrol movement pattern
	 */
	private static void generatePatrolPath(List<Location> path, Location center, int radius, int steps)
	{
		// Create patrol points in a square pattern
		Location[] patrolPoints = {
			new Location(center.getX() + radius, center.getY(), center.getZ()),
			new Location(center.getX(), center.getY() + radius, center.getZ()),
			new Location(center.getX() - radius, center.getY(), center.getZ()),
			new Location(center.getX(), center.getY() - radius, center.getZ())
		};
		
		for (int i = 0; i < steps; i++)
		{
			Location point = patrolPoints[i % patrolPoints.length];
			int z = GeoEngine.getInstance().getHeight(point.getX(), point.getY(), center.getZ());
			path.add(new Location(point.getX(), point.getY(), z));
		}
	}
	
	/**
	 * Generate spiral movement pattern
	 */
	private static void generateSpiralPath(List<Location> path, Location center, int radius, int steps)
	{
		for (int i = 0; i < steps; i++)
		{
			double angle = (2 * Math.PI * i) / 8; // Multiple rotations
			double distance = (radius * i) / steps; // Gradually increase distance
			
			int x = center.getX() + (int)(Math.cos(angle) * distance);
			int y = center.getY() + (int)(Math.sin(angle) * distance);
			int z = GeoEngine.getInstance().getHeight(x, y, center.getZ());
			
			path.add(new Location(x, y, z));
		}
	}
	
	/**
	 * Check for obstacles and adjust path
	 */
	public static List<Location> adjustPathForObstacles(List<Location> originalPath, Player player)
	{
		List<Location> adjustedPath = new ArrayList<>();
		
		if (originalPath.isEmpty())
		{
			return adjustedPath;
		}
		
		adjustedPath.add(originalPath.get(0));
		
		for (int i = 1; i < originalPath.size(); i++)
		{
			Location current = adjustedPath.get(adjustedPath.size() - 1);
			Location next = originalPath.get(i);
			
			if (!GeoEngine.getInstance().canMoveToTarget(current, next))
			{
				// Find alternative route
				Location alternative = findAlternativeRoute(current, next, player);
				if (alternative != null)
				{
					adjustedPath.add(alternative);
				}
			}
			
			adjustedPath.add(next);
		}
		
		return adjustedPath;
	}
	
	/**
	 * Find alternative route when direct path is blocked
	 */
	private static Location findAlternativeRoute(Location from, Location to, Player player)
	{
		for (int angle = 0; angle < 360; angle += 45)
		{
			double radians = Math.toRadians(angle);
			int distance = 150;
			
			int x = from.getX() + (int)(Math.cos(radians) * distance);
			int y = from.getY() + (int)(Math.sin(radians) * distance);
			int z = GeoEngine.getInstance().getHeight(x, y, from.getZ());
			
			Location alternative = new Location(x, y, z);
			
			if (GeoEngine.getInstance().canMoveToTarget(from, alternative) &&
				GeoEngine.getInstance().canMoveToTarget(alternative, to))
			{
				return alternative;
			}
		}
		
		return null;
	}
	
	/**
	 * Optimize path by removing unnecessary waypoints
	 */
	public static List<Location> optimizePath(List<Location> path)
	{
		if (path.size() <= 2)
		{
			return new ArrayList<>(path);
		}
		
		List<Location> optimized = new ArrayList<>();
		optimized.add(path.get(0));
		
		int i = 0;
		while (i < path.size() - 1)
		{
			int j = i + 1;
			
			// Find the furthest point we can reach directly
			while (j < path.size() && GeoEngine.getInstance().canMoveToTarget(path.get(i), path.get(j)))
			{
				j++;
			}
			
			optimized.add(path.get(j - 1));
			i = j - 1;
		}
		
		return optimized;
	}
	
	// Helper methods
	private static List<Location> getNeighborLocations(Location center)
	{
		List<Location> neighbors = new ArrayList<>();
		int step = 100; // Grid size
		
		for (int dx = -step; dx <= step; dx += step)
		{
			for (int dy = -step; dy <= step; dy += step)
			{
				if (dx == 0 && dy == 0) continue;
				
				int x = center.getX() + dx;
				int y = center.getY() + dy;
				int z = GeoEngine.getInstance().getHeight(x, y, center.getZ());
				
				neighbors.add(new Location(x, y, z));
			}
		}
		
		return neighbors;
	}
	
	private static boolean isInClosedList(Location location, List<PathNode> closedList)
	{
		return closedList.stream().anyMatch(node -> calculateDistance(node.location, location) < 50);
	}
	
	private static PathNode findInOpenList(Location location, List<PathNode> openList)
	{
		return openList.stream()
			.filter(node -> calculateDistance(node.location, location) < 50)
			.findFirst()
			.orElse(null);
	}
	
	private static List<Location> reconstructPath(PathNode endNode)
	{
		List<Location> path = new ArrayList<>();
		PathNode current = endNode;
		
		while (current != null)
		{
			path.add(0, current.location);
			current = current.parent;
		}
		
		return path;
	}
	
	/**
	 * Calculate distance between two locations
	 */
	private static double calculateDistance(Location loc1, Location loc2)
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