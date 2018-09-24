# Undergraduate Thesis
## Joint Source-Channel Encoding for Image Transmission over Wireless Networks

Implementation of our undergraduate thesis. 

EncoderFactory will create an encoder/decoder pair that has been optimized for the channel provided. The Encoder will map each coefficient of the image DCT to a single Channel-Optimzed Scalar Quantizer (COSQ) for encoding. COSQs will be allocated more bits depending on the importance of that DCT coefficient, as specified by the bit allocation matrix.
