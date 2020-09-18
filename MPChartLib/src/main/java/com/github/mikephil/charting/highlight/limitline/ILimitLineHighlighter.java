package com.github.mikephil.charting.highlight.limitline;

import com.github.mikephil.charting.components.LimitLine;

public interface ILimitLineHighlighter{

    LimitLine getHighlight(float x, float y);
}
