package com.droidtools.rubiksolver;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class HColor implements Comparable<HColor>, Parcelable {
	// These are accessible for performance.
	// Hue (Normalized to 0 to 255)
	public double h = 0;
	// Saturation (Normalized to 0 to 255)
	public double s = 0;
	// Luminance (Normalized to 0 to 255)
	public double l = 0;
	// Red
	public int r = 0;
	// Green
	public int g = 0;
    // Blue
	public int b = 0;
	
	public double distance = 0;
	public int key = -1;

	public HColor(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
		double[] res = hue(r, g, b);
		this.h = res[0];
		this.l = res[1];
		this.s = res[2];
	}

	public HColor(int color) {
		r = (color >> 16) & 0xFF;
		g = (color >> 8) & 0xFF;
		b = color & 0xFF;
		double[] res = hue(r, g, b);
		this.h = res[0];
		this.l = res[1];
		this.s = res[2];
	}
	
	public HColor(byte[] color) {
		int col = (color[0] << 24)
        	+ ((color[1] & 0xFF) << 16)
        	+ ((color[2] & 0xFF) << 8)
        	+ (color[3] & 0xFF);
		r = (col >> 16) & 0xFF;
		g = (col >> 8) & 0xFF;
		b = col & 0xFF;
		double[] res = hue(r, g, b);
		this.h = res[0];
		this.l = res[1];
		this.s = res[2];
		
	}

	public HColor(double h, double l, double s, int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.h = h;
		this.l = l;
		this.s = s;
	}
	
	

	/*
	public double getH() {
		return h;
	}

	public double getL() {
		return l;
	}

	public int getR() {
		return r;
	}

	public int getG() {
		return g;
	}

	public int getB() {
		return b;
	}*/

	public int getColor() {
		return 0xFF << 24 | r << 16 | g << 8 | b;
	}

	public boolean isBlack() {
		return r < 15 && g < 15 && b < 15;
	}
	
	public byte[] asByteArray() {
		int value = getColor();
		return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}

	private static double[] hue(int ri, int gi, int bi) {
		double[] ret = new double[3];
		double r = ri / 255.0;
		double g = gi / 255.0;
		double b = bi / 255.0;

		double maxcol = Math.max(Math.max(r, g), b);
		double mincol = Math.min(Math.min(r, g), b);
		ret[1] = (maxcol + mincol) / 2;
		
		if( maxcol != 0 )
			ret[2] = (maxcol-mincol) / maxcol;		// s
		else {
			// r = g = b = 0		// s = 0, v is undefined
			ret[2] = 0;
		}

		if (maxcol == mincol)
			ret[0] = 0;
		if (r == maxcol)
			ret[0] = (g - b) / (maxcol - mincol) + (g < b ? 6.0 : 0);
		else if (g == maxcol)
			ret[0] = 2.0 + (b - r) / (maxcol - mincol);
		else if (b == maxcol)
			ret[0] = 4.0 + (r - g) / (maxcol - mincol);
		else {
			ret[0] = 0;
		}

		ret[0] = ret[0] / 6 * 255;
		ret[1] = ret[1] * 255;
		ret[2] = ret[2] * 255;

		return ret;
	}

	protected double distance(HColor other) {
		double[] vals = {r-other.r, g-other.g, b-other.b};
		double sum = 0;
		for (int i = 0; i<vals.length; i++)
			sum += vals[i]*vals[i];
		return Math.sqrt(sum);
	}

	public Byte mostSimilar(List<Byte> ids, ColorDecoder decoder, int limit) {
		if (ids.size() == 0) return null;
		for (int i=0; i<ids.size(); i++)
			decoder.getColor(ids.get(i)).distance = distance(decoder.getColor(ids.get(i)));
		Collections.sort(ids, new DistanceComparable(decoder));
		HColor ret = decoder.getColor(ids.get(0));
		if (ret.distance > limit && limit != -1)
			return null;
		else
			return ids.get(0);
	}
	
	/**
	 * Updates this color by taking the average of this and other color.
	 * @param other color to be averaged with this
	 */
	public void usurp(HColor other) {
		this.r = (r+other.r)/2;
		this.g = (g+other.g)/2;
		this.b = (b+other.b)/2;
		this.h = (h+other.h)/2;
		this.l = (l+other.l)/2;
		this.s = (s+other.s)/2;
	}
	
	@Override
	public String toString() {
		return String.format("0x%X", getColor());
	}
	
	/**
	 * Compares two colors by comparing their hue. This is somewhat problematic because hue of 0
	 * and 255 are equal so a collection sorted by hue is circular in nature.
	 */
	@Override
	public int compareTo(HColor another) {
		// This gets called a lot so inline it.
		// return Double.compare(h, another.h);
		
		double other = another.h;
	    if (h < other)
	        return -1;       // Neither val is NaN, thisVal is smaller
	    if (h > other)
	        return 1;        // Neither val is NaN, thisVal is larger

	    long thisBits = Double.doubleToLongBits(h);
	    long anotherBits = Double.doubleToLongBits(other);

	    return (thisBits == anotherBits ?  0 : // Values are equal
	            (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
	             1));                          // (0.0, -0.0) or (NaN, !NaN)
	}

	private HColor(Parcel in) {
		int color = in.readInt();
		r = (color >> 16) & 0xFF;
		g = (color >> 8) & 0xFF;
		b = color & 0xFF;
		h = in.readDouble();
		l = in.readDouble();
		s = in.readDouble();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(getColor());
		out.writeDouble(h);
		out.writeDouble(l);
		out.writeDouble(s);
	}

	public static final Parcelable.Creator<HColor> CREATOR = new Parcelable.Creator<HColor>() {
		public HColor createFromParcel(Parcel in) {
			return new HColor(in);
		}

		public HColor[] newArray(int size) {
			return new HColor[size];
		}
	};

	private class DistanceComparable implements Comparator<Byte>{
		ColorDecoder mDecoder;
		
		public DistanceComparable(ColorDecoder decoder)
		{
			mDecoder = decoder;
		}
		
		@Override
		public int compare(Byte o1, Byte o2) {
			return (mDecoder.getColor(o1).distance>mDecoder.getColor(o2).distance ? 1 : (mDecoder.getColor(o1).distance==mDecoder.getColor(o2).distance ? 0 : -1));
		}
	}
}
