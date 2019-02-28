/*
 * Copyright [2013-2019] PayPal Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ml.shifu.shifu.core.dtrain.wnd;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ml.shifu.shifu.core.dtrain.AssertUtils;
import ml.shifu.shifu.util.Tuple;

/**
 * {@link WideLayer} defines wide part of WideAndDeep. It includes a list of {@link WideFieldLayer} instances (each one
 * is for each wide column) and one {@link BiasLayer}.
 * 
 * <p>
 * {@link SparseInput} is leveraged for wide columns as with one-hot encoding to save more computation.
 * 
 * @author Zhang David (pengzhang@paypal.com)
 */
public class WideLayer extends AbstractLayer<Tuple<List<SparseInput>, float[]>, float[], float[], List<float[]>>
        implements WeightInitializer {

    /**
     * Layers for all wide columns.
     */
    private List<WideFieldLayer> layers;

    /**
     * Layers for all wide columns.
     */
    private WideDenseLayer denseLayer;

    /**
     * Bias layer
     */
    private BiasLayer bias;

    public WideLayer() {
    }

    public WideLayer(List<WideFieldLayer> layers, BiasLayer bias) {
        this.layers = layers;
        this.bias = bias;
    }

    public WideLayer(List<WideFieldLayer> layers, WideDenseLayer denseLayer, BiasLayer bias) {
        this.layers = layers;
        this.bias = bias;
        this.denseLayer = denseLayer;
    }

    @Override
    public int getOutDim() {
        int len = 0;
        for(WideFieldLayer layer: getLayers()) {
            len += layer.getOutDim();
        }
        len += 1; // bias
        len += 1; // WideDenseLayer
        return len;
    }

    @Override
    public float[] forward(Tuple<List<SparseInput>, float[]> input) {
        AssertUtils.assertListNotNullAndSizeEqual(this.getLayers(), input.getFirst());
        float[] results = new float[layers.get(0).getOutDim()];
        for(int i = 0; i < getLayers().size(); i++) {
            float[] fOuts = this.getLayers().get(i).forward(input.getFirst().get(i));
            for(int j = 0; j < results.length; j++) {
                results[j] += fOuts[j];
            }
        }

        float[] denseForwards = this.denseLayer.forward(input.getSecond());
        assert denseForwards.length == results.length;
        for(int j = 0; j < results.length; j++) {
            results[j] += denseForwards[j];
        }

        for(int j = 0; j < results.length; j++) {
            results[j] += bias.forward(1f);
        }

        return results;
    }

    @Override
    public List<float[]> backward(float[] backInputs, float sig) {
        // below backward call is for gradients computation in WideFieldLayer and BiasLayer
        List<float[]> list = new ArrayList<>();
        for(int i = 0; i < getLayers().size(); i++) {
            list.add(this.getLayers().get(i).backward(backInputs, sig));
        }

        list.add(this.denseLayer.backward(backInputs, sig));
        list.add(new float[] { bias.backward(backInputs[0], sig) });
        return list;
    }

    /**
     * @return the layers
     */
    public List<WideFieldLayer> getLayers() {
        return layers;
    }

    /**
     * @param layers
     *            the layers to set
     */
    public void setLayers(List<WideFieldLayer> layers) {
        this.layers = layers;
    }

    /**
     * @return the bias
     */
    public BiasLayer getBias() {
        return bias;
    }

    /**
     * @param bias
     *            the bias to set
     */
    public void setBias(BiasLayer bias) {
        this.bias = bias;
    }

    @Override
    public void initWeight(InitMethod method) {
        for(WideFieldLayer layer: this.layers) {
            layer.initWeight(method);
        }
        this.denseLayer.initWeight(method);
        this.bias.initWeight(method);
    }

    public void initGrads() {
        for(WideFieldLayer layer: this.layers) {
            layer.initGrads();
        }
        this.denseLayer.initGrads();
        this.bias.initGrads();
    }

    /**
     * @return the denseLayer
     */
    public WideDenseLayer getDenseLayer() {
        return denseLayer;
    }

    /**
     * @param denseLayer
     *            the denseLayer to set
     */
    public void setDenseLayer(WideDenseLayer denseLayer) {
        this.denseLayer = denseLayer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ml.shifu.guagua.io.Bytable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        if(this.layers == null) {
            out.writeInt(0);
        } else {
            out.writeInt(this.layers.size());
            for(WideFieldLayer wideFieldLayer: this.layers) {
                wideFieldLayer.write(out, this.serializationType);
            }
        }

        if(this.denseLayer == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            this.denseLayer.write(out, this.serializationType);
        }

        if(this.bias == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            this.bias.write(out, this.serializationType);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ml.shifu.guagua.io.Bytable#readFields(java.io.DataInput)
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        int layerSize = in.readInt();
        if(this.layers == null) {
            this.layers = new ArrayList<WideFieldLayer>();
        }
        for(int i = 0; i < layerSize; i++) {
            if(this.layers.get(i) == null) {
                this.layers.add(i, new WideFieldLayer());
            }
            this.layers.get(i).readFields(in, this.serializationType);
        }
        while(this.layers.size() > layerSize) {
            this.layers.remove(layerSize);
        }

        boolean denseLayerExist = in.readBoolean();
        if(!denseLayerExist) {
            this.denseLayer = null;
        } else {
            if(this.denseLayer == null) {
                this.denseLayer = new WideDenseLayer();
            }
            this.denseLayer.readFields(in, this.serializationType);
        }

        boolean biasExist = in.readBoolean();
        if(!biasExist) {
            this.bias = null;
        } else {
            if(this.bias == null) {
                this.bias = new BiasLayer();
            }
            this.bias.readFields(in, this.serializationType);
        }
    }

}
