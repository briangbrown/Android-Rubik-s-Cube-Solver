package com.droidtools.rubiksolver.test;

import java.util.ArrayList;
import java.util.List;

import com.droidtools.rubiksolver.HColor;

import junit.framework.TestCase;

public class HColorTest extends TestCase{
	
	//private final String TAG = HColorTest.class.getName();
	
	public HColorTest() {
		super();
	}
	
	/**
	 * Tests that the HColor Average method works correctly.
	 */
	public void testAvg() {
		List<HColor> colors = new ArrayList<HColor>();
		colors.add(new HColor(255, 0, 0));
		colors.add(new HColor(0, 255, 0));
		colors.add(new HColor(0, 0, 255));
		
		HColor avgColor = HColor.rgbAverage(colors);
		assertEquals(85, avgColor.r);
		assertEquals(85, avgColor.g);
		assertEquals(85, avgColor.b);
	}
	
	/**
	 * Tests that the HColor Dominant Average method works correctly.
	 */
	public void testDominantAvg() {
		List<HColor> colors = new ArrayList<HColor>();
		colors.add(new HColor(255, 0, 0));
		colors.add(new HColor(0, 255, 0));
		colors.add(new HColor(0, 0, 255));
		colors.add(new HColor(0, 0, 255));
		
		HColor avgColor = HColor.hsvDominantAverage(colors);
		assertEquals(0, avgColor.r);
		assertEquals(0, avgColor.g);
		assertEquals(255, avgColor.b);
	}
}
