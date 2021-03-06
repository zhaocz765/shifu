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
package ml.shifu.shifu.core.dtrain.wdl.weight;

/**
 * Class Description.
 *
 * @author Wu Devin (haifwu@paypal.com)
 */
public class Zero implements Initialisable {

    /**
     * Init with one float number.
     *
     * @return the init number.
     */
    @Override
    public float initWeight() {
        return 0;
    }

    /**
     * Init a one dimensional float array with specific length
     *
     * @param length
     *          the length of the array
     * @return the init array
     */
    @Override
    public float[] initWeight(int length) {
        return new float[length];
    }

    /**
     * Init a two dimensional float array with specific row and column
     *
     * @param row
     *          the row number
     * @param col
     *          the column number
     * @return the init array
     */
    @Override
    public float[][] initWeight(int row, int col) {
        return new float[row][col];
    }
}
