package com.droidtools.rubiksolver.test;

import java.util.ArrayList;

import com.droidtools.rubiksolver.RubikCube;
import com.droidtools.rubiksolver.RubikCube.CubeState;
import com.droidtools.rubiksolver.RubikMove;

import junit.framework.TestCase;

public class RubikCubeTest extends TestCase{
	
	// private final String TAG = RubikCubeTest.class.getName();
	
	public RubikCubeTest() {
		super();
	}

	/**
	 * Tests that the simple solving algorithm works.
	 */
	public void testRandomSolve() {
		RubikCube cube = new RubikCube();
		cube.randomize(100);
		assertTrue(cube.solveCube());
	}
	
	/**
	 * Tests that we can play back a solution move sequence and solve a cube.
	 */
	public void testExecuteMoves() {
		RubikCube cube = new RubikCube();
		cube.randomize(100);
		
		// Copy the randomized cube state to a new cube.
		RubikCube cube1 = new RubikCube(cube.getCubeState());		
		assertTrue(cube.solveCube());
		ArrayList<RubikMove> solution = cube.getMoveList();
		
		boolean executeSolved = cube1.executeMoves(solution);
		ArrayList<RubikMove> executedMoves = cube1.getMoveList();
		
		assertEquals(solution, executedMoves);
		assertTrue(executeSolved);
	}
	
	/**
	 * Tests that the optimizeSolution method decreases the number of moved and still solves the
	 * cube.
	 */
	public void testOptimizeMoves() {
		RubikCube cube = new RubikCube();
		cube.randomize(100);
		
		// Copy the randomized cube state to a new cube.
		RubikCube cube1 = new RubikCube(cube.getCubeState());		
		assertTrue(cube.solveCube());
		ArrayList<RubikMove> solution = cube.getMoveList();
		
		ArrayList<RubikMove> optSolution = RubikCube.optimizeSolution(solution);
		// The optimized solution should have less moves than the unoptimized solution.
		assertTrue(optSolution.size() < solution.size());
		// Test that the optimized solution actually solves the cube.
		assertTrue(cube1.executeMoves(optSolution));
	}
	
	/**
	 * Tests that the cubeStateOptimization method decreases the number of moved and still solves
	 * the cube.
	 */
	public void testCubeStateOptimization() {
		RubikCube cube = new RubikCube();
		cube.randomize(100);
		
		// Copy the randomized cube state to a new cube.
		RubikCube cube1 = new RubikCube(cube.getCubeState());		
		assertTrue(cube.solveCube());
		ArrayList<RubikMove> solution = cube.getMoveList();
		ArrayList<CubeState> cubeList = cube.getCubeList();
		
		ArrayList<RubikMove> optSolution = RubikCube.cubeStateOptimization(cubeList, solution);
		// The optimized solution should have less moves than the unoptimized solution.
		assertTrue(optSolution.size() < solution.size());
		// Test that the optimized solution actually solves the cube.
		assertTrue(cube1.executeMoves(optSolution));
	}
}
