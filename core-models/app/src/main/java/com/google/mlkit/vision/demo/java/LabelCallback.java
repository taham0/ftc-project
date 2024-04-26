package com.google.mlkit.vision.demo.java;

import com.google.mlkit.vision.label.ImageLabel;

import java.util.List;

public interface LabelCallback {
    void onLabelCallback(List<ImageLabel> labels);
}
