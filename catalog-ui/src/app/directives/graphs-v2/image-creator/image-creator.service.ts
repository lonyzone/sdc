/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

export interface ICanvasImage {
    src: string;
    width: number
    height: number;
    x: number;
    y: number;
}

'use strict';
export class ImageCreatorService {
    static '$inject' = ['$q'];
    private _canvas:HTMLCanvasElement;

    constructor(private $q:ng.IQService) {
        this._canvas = <HTMLCanvasElement>$('<canvas>')[0];
        this._canvas.setAttribute('style', 'display:none');

        let body = document.getElementsByTagName('body')[0];
        body.appendChild(this._canvas);
    }

    /**
     * Create an image composed of different image layers
     * @param canvasImages
     * @param canvasWidth 
     * @param canvasHeight 
     * returns a PROMISE
     */
    getMultiLayerBase64Image(canvasImages: ICanvasImage[], canvasWidth?:number, canvasHeight?:number):ng.IPromise<string> {
        const deferred = this.$q.defer<string>();

        if(canvasImages && canvasImages.length === 0){
            return null;
        }

        //If only width was set, use it for height, otherwise use first canvasImage height
        canvasHeight = canvasHeight || canvasImages[0].height;
        canvasWidth = canvasWidth || canvasImages[0].width;

        const images = [];
        let imagesLoaded = 0;
        const onImageLoaded = () => {
            imagesLoaded++;
            if(imagesLoaded < canvasImages.length){
                return;
            }
            this._canvas.setAttribute('width', canvasWidth.toString());
            this._canvas.setAttribute('height', canvasHeight.toString());
            const canvasCtx = this._canvas.getContext('2d');
            canvasCtx.clearRect(0, 0, this._canvas.width, this._canvas.height);
            images.forEach((image, index) => {
                const canvasImage = canvasImages[index];
                canvasCtx.drawImage(image, canvasImage.x, canvasImage.y, canvasImage.width, canvasImage.height);
            });
            let base64Image = this._canvas.toDataURL();
            deferred.resolve(base64Image);
        };
        canvasImages.forEach(canvasImage => {
            let image = new Image();
            image.onload = onImageLoaded;
            image.src = canvasImage.src;
            images.push(image);
        });
        return deferred.promise;
    }
}
