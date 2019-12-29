package com.bmxgates.test;

import org.psoft.math.MathUtils;

import junit.framework.Assert;
import junit.framework.TestCase;


public class TestMathUtils extends TestCase {

	//less then 1 truncated off ends
	private static final long[] dataset1 = new long[] {1123, 1201, 998};
	
	//more the 1 truncate off ends
	private static final long[] dataset2 = new long[] {1123, 1201, 998, 1509, 975, 1023, 1176, 1345, 899, 1212, 1221, 1123, 1201, 998, 1509, 975, 1023, 1176, 1345, 899, 1212, 1221 };

	//exactly the 1 truncate off ends
	private static final long[] dataset3 = new long[] {1123, 1201, 998, 1509, 975, 1023, 1176, 1345, 899, 1212, 1221, 1123, 1201, 998, 1509, 975, 1023, 1176, 1345, 899 };

	public TestMathUtils() {
		super("TestMathUtils");
	}
	
	protected void setUp(){
	}

	public void testTrimmedRangeDataSet1(){
		long[] results = MathUtils.trimmedRange(dataset1,  0.05);
		
		Assert.assertEquals(2, results.length);
		Assert.assertEquals(1061, results[0]);
		Assert.assertEquals(1162, results[1]);
	}
	
	public void testTrimmedMeanDataSet1(){
		long result = MathUtils.trimmedMean(dataset1,  0.05);

		Assert.assertEquals(1108, result);
	}

	public void testTrimmedRangeDataSet2(){
		long[] results = MathUtils.trimmedRange(dataset2,  0.05);
		
		Assert.assertEquals(2, results.length);
		Assert.assertEquals(937, results[0]);
		Assert.assertEquals(1345, results[1]);
	}
	
	public void testTrimmedMeanDataSet2(){
		long result = MathUtils.trimmedMean(dataset2,  0.05);

		Assert.assertEquals(1072, result);
	}

	public void testTrimmedRangeDataSet3(){
		long[] results = MathUtils.trimmedRange(dataset3,  0.05);
		
		Assert.assertEquals(2, results.length);
		Assert.assertEquals(899, results[0]);
		Assert.assertEquals(1509, results[1]);
	}
	
	public void testTrimmedMeanDataSet3(){
		long result = MathUtils.trimmedMean(dataset3,  0.05);

		Assert.assertEquals(1140, result);
	}
}
