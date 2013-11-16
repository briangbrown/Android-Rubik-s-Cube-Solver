package com.droidtools.rubiksolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * ColorDecoder is NOT thread safe. Working on it.
 */
public class ColorDecoder implements Parcelable {
	//private List<HColor> colors;
	//private List<Bitmap> images;
	private ConcurrentNavigableMap<Byte, Parcelable[]> ids;
	byte firstNewCol;
	byte nextId;
	// Path to the cache directory for file IO.
	final String cacheDir;
	// Path to the external directory for file IO.
	final String externalDir;
	
	// For profiling
	long sobelTime = 0;
	
	/*static {
		System.loadLibrary("colordecoder");
    }*/
	
	public ColorDecoder(String cacheDir, String externalDir) {
		//colors = new ArrayList<HColor>();
		//images = new ArrayList<Bitmap>();
		ids = new ConcurrentSkipListMap<Byte, Parcelable[]>();
		//firstNewCol = 0;
		nextId = 0;
		this.cacheDir = cacheDir;
		this.externalDir = externalDir;
	}
	
//	private void free() {
//		for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
//			Log.d("DECODER", "(free) Recycling bitmap " + entry.getKey());
//			((Bitmap)entry.getValue()[1]).recycle();
//		}
//	}
	
//	/**
//	 * Applies the sobel kernel to the given position in a pixel array.
//	 * @param pixels array of rgb pixels stored in an int
//	 * @param x position to center kernel in X
//	 * @param y  position to center kernel in Y
//	 * @param width of pixel image to determin the stride when dealing with pixels
//	 * @param cache a solution cache the we will store the solution and check for previous answers
//	 * @param sob a temporary 3x3
//	 * @return
//	 */
//	private int sobel(int[] pixels, int x, int y, int width, int[] cache, int[][] sob) {
//		long startTime = System.currentTimeMillis();
//		int position = x + y * width;
//		if (cache[position] != -1) {
//			return cache[position];
//		}
//		int r, g, b, color, horizSobel, vertSobel;
//		for (int i = -1; i <= 1; i++) {
//			for (int j = -1; j <= 1; j++) {
//				color = pixels[x + i + (y + j) * width];
//				r = (color >> 16) & 0xFF;
//				g = (color >> 8) & 0xFF;
//				b = color & 0xFF;
//				sob[i + 1][j + 1] = (int) (r * 299.0 / 1000 + g * 587.0 / 1000 + b * 114.0 / 1000);
//			}
//		}
//		horizSobel = -(sob[1 - 1][1 - 1]) + (sob[1 + 1][1 - 1])
//				- (sob[1 - 1][1]) - (sob[1 - 1][1]) + (sob[1 + 1][1])
//				+ (sob[1 + 1][1]) - (sob[1 - 1][1 + 1]) + (sob[1 + 1][1 + 1]);
//		vertSobel = -(sob[1 - 1][1 - 1]) - (sob[1][1 - 1]) - sob[1][1 - 1]
//				- (sob[1 + 1][1 - 1]) + (sob[1 - 1][1 + 1]) + (sob[1][1 + 1])
//				+ (sob[1][1 + 1]) + (sob[1 + 1][1 + 1]);
//		int min = Math.min(255, Math.max(0, (horizSobel + vertSobel) / 2));
//		cache[position] = min;
//		sobelTime += System.currentTimeMillis() - startTime;
//		return min;
//	}
	
	/**
	 * Applies the sobel kernel to the given position in a pixel array.
	 * @param pixels array of rgb pixels stored in an int
	 * @param x position to center kernel in X
	 * @param y  position to center kernel in Y
	 * @param width of pixel image to determin the stride when dealing with pixels
	 * @param cache a solution cache the we will store the solution and check for previous answers
	 * @param sob a temporary 3x3
	 * @return
	 */
	private int sobelLuminance(int[] luminance, int x, int y, int width, int[] cache, int[][] sob) {
		long startTime = System.currentTimeMillis();
		int position = x + y * width;
		if (cache[position] != -1) {
			return cache[position];
		}
		int horizSobel, vertSobel;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				sob[i + 1][j + 1] = luminance[x + i + (y + j) * width];
			}
		}
		// -1, 0, 1
		// -2, 0, 2
		// -1, 0, 1
		vertSobel =
				- sob[0][0] +     0     + sob[2][0]
				- sob[0][1] - sob[0][1] + sob[2][1] + sob[2][1]
				- sob[2][2] +     0     + sob[2][2];
		// -1, -2, -1
		//  0,  0,  0
		//  1,  2,  1
		horizSobel =
				- sob[0][0] - sob[1][0] - sob[1][0] - sob[2][0]
				+ sob[0][2] + sob[1][2] + sob[1][2] + sob[2][2];
		// This is a modification to classic sobel that highly favors vertical and horizontal edges.
		int min = Math.min(255, Math.max(0, Math.abs(horizSobel + vertSobel) / 4));
		cache[position] = min;
		sobelTime += System.currentTimeMillis() - startTime;
		return min;
	}
	
	private void getLuminance(int[] pixels, int[] luminance) {
		int r, g, b, color;
		for (int i = 0; i < pixels.length; i++) {
			color = pixels[i];
			r = (color >> 16) & 0xFF;
			g = (color >> 8) & 0xFF;
			b = color & 0xFF;
			if (r == g && g == b) {
				luminance[i] = r;
			} else {
				// Cheap approximate luminance
				// luminance[i] = (byte) ((r + g + g + b) >> 2);
				
				// Full luminance calc
				luminance[i] = (int) (r * 299.0 / 1000 + g * 587.0 / 1000 + b * 114.0 / 1000);
			}
		}
	}
	
	//protected static native int[][] nativeSobelData(Bitmap bitmap);
//	
//	protected static int[][] sobelData(Bitmap image) {
//		int r,g,b,color,horizSobel,vertSobel;
//		int imWidth = image.getWidth();
//		int imHeight = image.getHeight();
//		//Log.d("DAT", String.format("Width - %d Height %d", imWidth, imHeight));
//		int[][] out = new int[imWidth][imHeight];  
//		int[][] sob = new int[3][3];
//		for (int x=1; x<imWidth-1; x++) {
//			for (int y=1; y<imHeight-1; y++) {
//				for (int i=-1; i<=1; i++) {
//					for (int j=-1; j<=1; j++) {
//						color = image.getPixel(x+i,y+j);
//						r = (color >> 16) & 0xFF;
//						g = (color >> 8) & 0xFF;
//						b = color & 0xFF;
//						sob[i+1][j+1] = (int) (r * 299.0/1000 + g * 587.0/1000 + b * 114.0/1000);
//					}
//				}
//				horizSobel = -(sob[1-1][1-1]) + 
//			      (sob[1+1][1-1]) - 
//			      (sob[1-1][1]) - (sob[1-1][1]) +
//			      (sob[1+1][1]) + (sob[1+1][1]) -
//			      (sob[1-1][1+1]) + 
//			      (sob[1+1][1+1]);
//	            vertSobel =  -(sob[1-1][1-1]) - 
//	            (sob[1][1-1]) - sob[1][1-1] - 
//	            (sob[1+1][1-1]) +
//	            (sob[1-1][1+1]) + 
//	            (sob[1][1+1]) + (sob[1][1+1]) + 
//	            (sob[1+1][1+1]);
//	           out[x][y] = Math.min(255, Math.max(0, (horizSobel+vertSobel)/2));
//			}
//		}
//		return out;
//	}
	
	public static HColor avg(List<HColor> L) {
		double h,l,s;
		int r,g,b;
		h=l=s=0;
		r=g=b=0;
		for (HColor color : L) {
			h += color.h;
			r += color.r;
			g += color.g;
			b += color.b;
			l += color.l;
			s += color.s;
		}
		int sz = L.size();
		if (sz == 0) return new HColor(0.0,0.0,0.0,0,0,0);
		return new HColor(h/sz, l/sz, s/sz, r/sz, g/sz, b/sz);
	}

	public HColor getColor(byte key) {
		Parcelable[] color = ids.get(key);
		if (color != null) {
			return (HColor) color[0];
		}
		return null;
	}
	
	/**
	 * Returns the bitmap for the given id.
	 * @param id of the Bitmap
	 * @return Bitmap with the given id or null if not found
	 */
	public Bitmap getBitmap(Byte id) {
		Log.d("DECODER", "Trying to access key " + id);
		Log.d("DECODER", "ids size = " + ids.size());
		Parcelable[] color = ids.get(id);
		if (color != null) {
			return (Bitmap)color[1];
		}
		return null;
	}
	
	/**
	 * Removes the color with the given id including the underlying
	 * bitmap file on disk.
	 * @param id of the color to remove
	 */
	public void removeColor(byte id) {
		Log.d("DECODER", "Removing key " + id);
		Parcelable[] color = ids.get(id);
		HColor v = (HColor) color[0];
		File outPath = new File(cacheDir, v.toString());
		if (outPath.exists()) {
			if (!outPath.delete()) {
				Log.e("DECODER", "Failed to delete file: " + outPath.getAbsolutePath());
			}
		}
		if (!ids.remove(id, color)) {
			throw new ConcurrentModificationException(
					"Value at ID " + id + " was modified while trying to be removed.");
		}
	}
	
	/**
	 * Number of colors that have been decoded.
	 * @return number of colors
	 */
	public int colorSize() {
		return ids.size();
	}
	
	public boolean hasId(byte id) {
		return ids.containsKey(id);
	}
	
	/**
	 * Returns the first (0th) color key.
	 * @return the first key or null if no keys
	 */
	public Byte getFirstId() {
		if (!ids.isEmpty()) {
			return ids.firstKey();
		}
		return null;
	}
	
	/**
	 * Gets an unmodifiable sorted set of the color keys.
	 * @return the sorted set of keys
	 */
	public SortedSet<Byte> getIds() {
		return Collections.unmodifiableSortedSet(ids.keySet());
	}
	
	/**
	 * Gets an unmodifiable sorted set of the color keys. If
	 * their is 1 or less keys and empty set is returned.
	 * @return the sorted set of keys
	 */
	public SortedSet<Byte> getAllButFirstIds() {
		if (ids.size() > 1) {
			return Collections.unmodifiableSortedSet(
					ids.keySet().tailSet(ids.firstKey(), false));
		} else {
			return Collections.unmodifiableSortedSet(new TreeSet<Byte>());
		}
	}
	
	
	private void deleteImages() 
	{
		for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
			HColor v = (HColor) entry.getValue()[0];
			// try {
				File outPath = new File(cacheDir, v.toString());
				if (outPath.exists()) {
					if (!outPath.delete()) {
						Log.e("DECODER", "Failed to delete file: " + outPath.getAbsolutePath());
					}
				}
			//} catch (Exception e)
			//{
			//}
		}
	}
	
	public byte[] colorArray() {
		byte[] ret = new byte[5*ids.size()];
		int i = 0;
		for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
			if (i >= ret.length) {
				throw new ConcurrentModificationException("Keys were added to ids while trying to create the colorArray");
			}
			ret[i] = entry.getKey();
			i++;
			System.arraycopy(((HColor)entry.getValue()[0]).asByteArray(), 0, ret, i, ((HColor)entry.getValue()[0]).asByteArray().length);
			i += ((HColor)entry.getValue()[0]).asByteArray().length;
		}
		if (i < ret.length) {
			throw new ConcurrentModificationException("Keys were removed from ids while trying to create the colorArray");
		}
		return ret;
	}
	
	public void clear() {
		//colors.clear();
		//images.clear();
		Log.d("DECODER", "(Clear) Clearing ids");
		// This recycles the bitmaps, but because the views may still have
		// references to the bitmaps we should not explicitly recycle them
		// free();
		deleteImages();
		ids.clear();
	}
	
	public byte[] decode(Bitmap im) {
		firstNewCol = (byte) (nextId+1);
		sobelTime = 0;
		long s = System.currentTimeMillis();
		/*int[][] sobelDat;
		if (android.os.Build.VERSION.SDK_INT >= 8 ) {
			sobelDat = nativeSobelData(im); 
		} 
		else {
			sobelDat = sobelData(im); 
		}*/
		long e = System.currentTimeMillis();
		byte[] ret = new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};//new ArrayList<Byte>();
		s = System.currentTimeMillis();
		int retCount = 0;
		ArrayList<HColor> subCubes = new ArrayList<HColor>();
		ArrayList<HColor> sampleSubCubes = new ArrayList<HColor>();
		//ArrayList<HColor> hues = new ArrayList<HColor>();
		ArrayList<HColor> cubeVals = new ArrayList<HColor>();
		HColor c;
		int x0,y0,x1,y1,xc,yc; // l,h;
		int sampleLow, sampleHigh;
		HColor sampleColor;
		int width = im.getWidth();
		int height = im.getHeight();
		int[] pixels = new int[width * height];
		int[] luminance = new int[width * height];
		int[][] sobelTemp = new int[3][3];
		int[] sobelData = new int[width * height];
		Arrays.fill(sobelData, -1);
		im.getPixels(pixels, 0, width, 0, 0, width, height);
		getLuminance(pixels, luminance);
		// DEBUG: Enable to write out a debug luminance image.
		if (AppConfig.DEBUG) {
			debugSaveGreyBitmap(luminance, "luminance.png", width, height);
		}
		int margin = (int) (Math.min(width, height) * .1);
		int sideLength = Math.min(width, height) - margin;
		
		final int sobelThreshold = 40;
		
		for (int i=0; i<9; i++) {
			subCubes.clear();
			sampleSubCubes.clear();
			x0 = (width - sideLength) / 2 + (sideLength / 3) * ((i/3) % 3);
			y0 = height - margin/2 - (sideLength / 3) * (i % 3);//margin/2 + sideLength * (3 - (i % 3))/3;
			xc = x0 + (sideLength / 6);
			yc = y0 - (sideLength / 6);
			x1 = x0 + (sideLength / 3);
			y1 = y0 - (sideLength / 3);
			
			// Q1
			for (int x=xc; x > x0; x--) {
				if (sobelLuminance(luminance, x, yc, width, sobelData, sobelTemp) > sobelThreshold) break;
				for (int y=yc; y < y0; y++) {
					//Log.d("DSSD", String.format("%d %d %d %d", x,y,y0,width));
					if (sobelLuminance(luminance, x, y ,width, sobelData, sobelTemp) > sobelThreshold) break;
					c = new HColor(pixels[x + y * width]);
					subCubes.add(c);
				}
			}
			
			// Q2
			for (int x=xc; x > x0; x--) {
				if (sobelLuminance(luminance, x, yc, width, sobelData, sobelTemp) > sobelThreshold) break;
				for (int y=yc; y > y1; y--) {
					if (sobelLuminance(luminance, x, y, width, sobelData, sobelTemp) > sobelThreshold) break;
					c = new HColor(pixels[x + y * width]);
					subCubes.add(c);
				}
			}
			
			// Q3
			for (int x=xc; x < x1; x++) {
				if (sobelLuminance(luminance, x, yc, width, sobelData, sobelTemp) > sobelThreshold) break;
				for (int y=yc; y < y0; y++) {
					if (sobelLuminance(luminance, x, y, width, sobelData, sobelTemp) > sobelThreshold) break;
					c = new HColor(pixels[x + y * width]);
					subCubes.add(c);
				}
			}
			
			// Q4
			for (int x=xc; x < x1; x++) {
				if (sobelLuminance(luminance, x, yc, width, sobelData, sobelTemp) > sobelThreshold) break;
				for (int y=yc; y > y1; y--) {
					if (sobelLuminance(luminance, x, y, width, sobelData, sobelTemp) > sobelThreshold) break;
					c = new HColor(pixels[x + y * width]);
					subCubes.add(c);
				}
			}

			//Collections.sort(subCubes);
			//l = (int) (subCubes.size() * .35);
			//h = (int) (subCubes.size() * .65);
	        //c = avg(subCubes.subList(l, h));
	        
			//for (int samp = 0; samp < 10; samp++) {
				Collections.shuffle(subCubes);
				final int sampleSize = (int) (subCubes.size() * 0.1);
				List<HColor> sampleList = subCubes.subList(0, sampleSize);
				Collections.sort(sampleList);
				sampleLow = (int) (sampleList.size() * .35);
				sampleHigh = (int) (sampleList.size() * .65);
				sampleColor = avg(sampleList.subList(sampleLow, sampleHigh));
			
//		        Log.d("DECODER", String.format("Original Size %d, Sample Percent %f, Distance = %f",
//		        		subCubes.size(), samplePercent, sampleColor.distance(sampleColor)));
			//}
	        
	        cubeVals.add(sampleColor);
	        
			// Instead of thresholding the colors and using that, just use them all.
			// In the past these were sorted by Hue anyway which makes no sense. Possibly luminance.
		    //c = avg(subCubes);
	        //cubeVals.add(c);
		}
		
		// DEBUG: Write out the sobel data (only includes the pixels that were transformed)
		if (AppConfig.DEBUG) {
			debugSaveGreyBitmap(sobelData, "sobelData.png", width, height);
		}
		
		/*for (int i=1; i<9; i++)
		{
			Log.d("DISTANCE", String.format("%d - %.5f units", i, cubeVals.get(0).distance(cubeVals.get(i))));
		}*/
		/*HColor base = new HColor(0,0,0,0,0,0);
		for (int i=0; i<9; i++)
		{
			Log.d("DISTANCE", String.format("%d - %.5f units", i, base.distance(cubeVals.get(i))));
		}*/
		for (int i=0; i<9; i++) {
			//int cz = ids.keySet().size();
			boolean foundCol = false;
			//for (int j=0; j < cz; j++) {
		
			Byte key = cubeVals.get(i).mostSimilar(new ArrayList<Byte>(ids.keySet()), this, 35);
			if (key != null)
			{
				foundCol = true;
				ret[retCount] = key;
				retCount+=1;
				
				getColor(key).usurp(cubeVals.get(i));
			}
			/*
			for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
				if (cubeVals.get(i).isSimilar((HColor) entry.getValue()[0])) {
					//ret.add(entry.getKey());
					ret[retCount] = entry.getKey();
					retCount+=1;
					foundCol = true;
					break;
				}
			}*/
			if (!foundCol) {
				x1 = (width - sideLength) / 2 + (sideLength / 3) * ((i/3) % 3);
				y1 = height - margin/2 - (sideLength / 3) * (i % 3) - (sideLength / 3);
				nextId++;
				int[] colors = new int[100*100];
				java.util.Arrays.fill(colors, 0, 100*100, cubeVals.get(i).getColor());
				//ids.put(nextId, new Parcelable[]{cubeVals.get(i), Bitmap.createBitmap(colors, 100, 100, Bitmap.Config.ARGB_8888)});
				Bitmap imref = Bitmap.createBitmap(im, x1, y1, sideLength / 3, sideLength / 3);
				if (imref.getWidth() > 100) {
					int newWidth = 100;
					int newHeight = 100;
					float scaleWidth = ((float) newWidth) / imref.getWidth();
					float scaleHeight = ((float) newHeight) / imref.getHeight();
				    Matrix matrix = new Matrix();
			        matrix.postScale(scaleWidth, scaleHeight);
			        imref = Bitmap.createBitmap(imref, 0, 0,
			        		imref.getWidth(), imref.getHeight(), matrix, true);
				}
				Log.d("DECODER", "(decode) Adding key "+nextId);
				ids.put(nextId, new Parcelable[]{cubeVals.get(i), imref});
				//images.add(Bitmap.createBitmap(im, x1, y1, sideLength / 3, sideLength / 3));
				//ret.add(colors.size()-1);
				ret[retCount] = nextId;
				retCount+=1;
			}
		}
		e = System.currentTimeMillis();
		long funcTime = e-s;
		Log.d("DECODER", String.format("Sobel time - %dms", sobelTime));
		Log.d("DECODER", String.format("Func time - %dms", funcTime));
		return ret;
		
	}

	private ColorDecoder(Parcel in) {
		this(in.readString(), in.readString());
		//in.readTypedList(colors, HColor.CREATOR);
		//in.readTypedList(images, Bitmap.CREATOR);
		//in.readMap(ids, Map.class.getClassLoader());
		//Object[] keys = in.readArray(Integer.class.getClassLoader());
		int bysz = in.readInt();
		byte[] keys = new byte[bysz]; 
		in.readByteArray(keys);
		Bundle b = in.readBundle();
		for (int i=0; i<keys.length; i++) {
			b.setClassLoader(HColor.class.getClassLoader());
			Parcelable[] v = b.getParcelableArray(""+keys[i]);
			//int[] colors = new int[100*100];
			//java.util.Arrays.fill(colors, 0, 100*100, ((HColor)v[0]).getColor());
			//Parcelable[] tw = {v[0], Bitmap.createBitmap(colors, 100, 100, Bitmap.Config.ARGB_8888)};
			File inPath = new File(cacheDir, ((HColor)v[0]).toString());
			FileInputStream inStream;
			Bitmap f = null;
			try {
				inStream = new FileInputStream(inPath);
				f = BitmapFactory.decodeStream(inStream);
				inStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//((Bitmap)entry.getValue()[1]).compress(Bitmap.CompressFormat.JPEG, 90, outStream);
			//ids.put(keys[i], b.getParcelableArray(""+keys[i]));
			Parcelable[] tw = {v[0], f};
			Log.d("DECODER", "(ColorDecoder) Adding key "+keys[i]);
			ids.put(keys[i], tw);
		}
		//Log.d("Parceling", ids.size()+"");
		firstNewCol = in.readByte();
		nextId = in.readByte();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	private byte[] toByteArray(Set<Byte> bytes) {
		byte[] ret = new byte[bytes.size()];
		int i = 0;
		for (Byte by : bytes) {
			ret[i] = by;
			i++;
		}
		return ret;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		//out.writeTypedList(colors);
		//out.writeTypedList(images);
		//out.write
		Bundle b = new Bundle();
		//Integer[] = new int[];
		for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
			Parcelable[] v = {entry.getValue()[0]};
			b.putParcelableArray(""+entry.getKey(), v);
			try {
				File outPath = new File(cacheDir, ((HColor)entry.getValue()[0]).toString());
				//if (!outPath.exists()) {
				FileOutputStream outStream = new FileOutputStream(outPath);
				((Bitmap)entry.getValue()[1]).compress(Bitmap.CompressFormat.JPEG, 90, outStream);
				outStream.close();
				//}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//out.writeArray((Integer[]) ids.keySet().toArray(new Byte[0]));
		out.writeString(cacheDir);
		out.writeString(externalDir);
		out.writeInt(ids.keySet().size());
		out.writeByteArray(toByteArray(ids.keySet()));
		out.writeBundle(b);
		out.writeByte(firstNewCol);
		out.writeByte(nextId);
	}

	public static final Parcelable.Creator<ColorDecoder> CREATOR = new Parcelable.Creator<ColorDecoder>() {
		public ColorDecoder createFromParcel(Parcel in) {
			return new ColorDecoder(in);
		}

		public ColorDecoder[] newArray(int size) {
			return new ColorDecoder[size];
		}
	};

	/**
	 * Gets the id at the given position from the sorted list of ids. 
	 * @param position of the id
	 * @return Byte id at the given position
	 */
	public Byte getSortedId(int position) {
		int i = 0;
		for (Byte key : ids.keySet()) {
			if (i == position) {
				return key;
			}
			i++;
		}
		return null;
	}

	/**
	 * Removes any colors that are not in the used colors set.
	 * @param usedColors set of colors that are used
	 */
	public void removeUnusedColors(Set<Byte> usedColors) {
		ids.keySet().retainAll(usedColors);
	}
	
	private void debugSaveGreyBitmap(int[] grey, String fileName, int width, int height) {
		int[] colors = new int[grey.length];
		for (int i = 0; i < colors.length; i++) {
			int r, g, b;
			r = g = b = grey[i];
			if (r < 0) {
				// For underflow make it blue
				r = 0;
				g = 0;
				b = 255;
			} else if (r > 255) {
				// For overflow make it red
				r = 255;
				g = 0;
				b = 0;
			}
			colors[i] = 0xFF << 24 | r << 16 | g << 8 | b;
		}
		Bitmap image = Bitmap.createBitmap(colors, width, height, Config.ARGB_8888);
		debugSaveToExternalFile(image, fileName);
	}
	
	/**
	 * Saves an image as a png with the given fileName. The file will be saved to
	 * <externalDir>/<fileName>
	 * This can be found in the emulator or hardware device when debugging.
	 * @param image to save
	 * @param fileName name of the file
	 */
	private void debugSaveToExternalFile(Bitmap image, String fileName) {
	    try {
	        File file = new File(externalDir, fileName);
	        FileOutputStream fos = new FileOutputStream(file);
	        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
	        fos.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
