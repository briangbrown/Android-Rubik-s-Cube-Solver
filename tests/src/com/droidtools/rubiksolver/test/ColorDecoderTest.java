package com.droidtools.rubiksolver.test;

import java.util.ArrayList;
import java.util.List;

import com.droidtools.rubiksolver.ColorDecoder;
import com.droidtools.rubiksolver.HColor;
import junit.framework.TestCase;

public class ColorDecoderTest extends TestCase {

	public ColorDecoderTest() {
		super();
	}
	
	public void testAvg() {
		List<HColor> colors = new ArrayList<HColor>();
		colors.add(new HColor(255, 0, 0));
		colors.add(new HColor(0, 255, 0));
		colors.add(new HColor(0, 0, 255));
		
		HColor avgColor = ColorDecoder.avg(colors);
		assertEquals(85, avgColor.r);
		assertEquals(85, avgColor.g);
		assertEquals(85, avgColor.b);
	}
	
}
