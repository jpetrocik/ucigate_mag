package com.bmxgates;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import com.bmxgates.database.GateSession;
import com.bmxgates.database.GateSession.GateTime;

public class SessionGraphFragment{

	
	/**
	 * Sets up the chart that display historical times
	 */
	public static View initChart(Context context, GateSession session){

		XYSeries series = new XYSeries("Times");
		XYSeries min = new XYSeries("Times");
		XYSeries max = new XYSeries("Times");
		int i=0;
		for (GateTime gt : session.getHistory()){
			series.add(i, gt.getTime());
			min.add(i, session.best());
			max.add(i, session.worst());
			i++;
		}

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);
		dataset.addSeries(max);
		dataset.addSeries(min);
		
		XYSeriesRenderer seriesRender = new XYSeriesRenderer();
		seriesRender.setShowLegendItem(false);
		XYSeriesRenderer minRender = new XYSeriesRenderer();
		minRender.setShowLegendItem(false);
		minRender.setColor(Color.RED);
		XYSeriesRenderer maxRender = new XYSeriesRenderer();
		maxRender.setShowLegendItem(false);
		maxRender.setColor(Color.RED);
		
		XYMultipleSeriesRenderer mSeriesRender = new XYMultipleSeriesRenderer();
		mSeriesRender.addSeriesRenderer(seriesRender);
		mSeriesRender.addSeriesRenderer(minRender);
		mSeriesRender.addSeriesRenderer(maxRender);
		mSeriesRender.setYAxisMin(.5);
		mSeriesRender.setShowAxes(false);
		mSeriesRender.setShowGrid(false);
		mSeriesRender.setShowLabels(false);
		mSeriesRender.setZoomEnabled(false, false);
		mSeriesRender.setPanEnabled(false, false);
		
		GraphicalView graphicalView = ChartFactory.getLineChartView(context, dataset, mSeriesRender);
	    graphicalView.setBackgroundResource(R.color.black);
	    return graphicalView;
	}
	
}
