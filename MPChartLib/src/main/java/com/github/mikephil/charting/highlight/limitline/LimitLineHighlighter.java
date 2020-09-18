package com.github.mikephil.charting.highlight.limitline;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;

public class LimitLineHighlighter<T extends BarLineScatterCandleBubbleDataProvider> implements ILimitLineHighlighter{

    /**
     * instance of the data-provider
     */
    protected T mChart;

    protected List<LimitLine> mLimitLineBuffer = new ArrayList<>();

    public LimitLineHighlighter(T chart){
        this.mChart = chart;
    }

    @Override
    public LimitLine getHighlight(float x, float y){
        List<LimitLine> lls = getHighlightsAtXValue(0f, x, y);
        if(lls.isEmpty()){
            return null;
        } else{
            return lls.get(0);
        }

    }

    protected List<LimitLine> getHighlightsAtXValue(float xVal, float x, float y){

        mLimitLineBuffer.clear();

        List<LimitLine> limitLines = mChart.getLimitLines();

        if(limitLines == null || limitLines.isEmpty()){
            return mLimitLineBuffer;
        }

        Transformer mTrans = mChart.getTransformer(YAxis.AxisDependency.LEFT);
        ViewPortHandler viewPortHandler = mChart.getViewPortHanl();

        for(int i = 0; i < limitLines.size(); i++){
            LimitLine ll = limitLines.get(i);
            float iconSize = ll.getIconSize() / 2f;

            float[] points = new float[2];
            points[0] = ll.getLimit();
            points[1] = 0f;
            mTrans.pointValuesToPixel(points);

            float limitLineY = (viewPortHandler.contentTop() - ll.getIconOffset());
            if(points[0] - iconSize < x && x < points[0] + iconSize){
                if(limitLineY - iconSize < y && y < limitLineY + iconSize){
                    mLimitLineBuffer.add(ll);
                    return mLimitLineBuffer;
                }
            }
        }

        return mLimitLineBuffer;
    }
}
