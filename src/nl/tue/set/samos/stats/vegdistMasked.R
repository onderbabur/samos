#
# Copyright (c) 2015-2021 Onder Babur
# 
# This file is part of SAMOS Model Analytics and Management Framework.
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this 
# software and associated documentation files (the "Software"), to deal in the Software 
# without restriction, including without limitation the rights to use, copy, modify, 
# merge, publish, distribute, sublicense, and/or sell copies of the Software, and to 
# permit persons to whom the Software is furnished to do so, subject to the following 
# conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies
#  or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
# INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
# PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
# CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
# THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# 
# @author Onder Babur
# @version 1.0
#

# This is modified from the vegan package. @date 01.01.2021 

`vegdistMasked` <-
    function (x, y, method = "bray", binary = FALSE, diag = FALSE, upper = FALSE, 
              na.rm = FALSE, ...) 
{
    library(bigmemory)
    print("Before loading vsm")
    print(gc())
    x <- as.matrix(x)
    y <- as.matrix(y)
    #x <- as.matrix(read.big.matrix("/Users/obabur/eclipse-cluster/workspace-master/model.cluster/tdm/ATLZoo/all/tdm-STR1-ATTRIBUTED-UNIGRAM-WEIGHT_1-NO_IDF-RELAXED_TYPE-REDUCED_SYNONYM-SYN80-FIX-CTX_STRICT-FREQ_SUM.csv", header=TRUE, type='double'))
    #y <- as.matrix(read.big.matrix("/Users/obabur/eclipse-cluster/workspace-master/model.cluster/tdm/ATLZoo/all/tdm-STR1-ATTRIBUTED-UNIGRAM-RAW-NO_IDF-STRICT_TYPE-NO_SYNONYM-NO_WORDNET-FIX-CTX_STRICT-FREQ_SUM.csv", header=TRUE, type='double'))
    #n <- 10000
    #print(n)
    #x <- x[c(1:n),c(1:n)]
    #gc()
    #y <- y[c(1:n),c(1:n)]
    #gc()
    print("After loading vsm")
    print(gc())
    #x <- x[c(1:300),]
    #y <- y[c(1:300),]
    ZAP <- 1e-15
    if (!is.na(pmatch(method, "euclidian"))) 
        method <- "euclidean"
    METHODS <- c("manhattan", "euclidean", "canberra", "bray", 
                 "kulczynski", "gower", "morisita", "horn", "mountford", 
                 "jaccard", "raup", "binomial", "chao", "altGower", "cao",
                 "mahalanobis")
    method <- pmatch(method, METHODS)
    inm <- METHODS[method]
    if (is.na(method)) 
        stop("invalid distance method")
    if (method == -1) 
        stop("ambiguous distance method")
    if (!method %in% c(1,2,6,16) && any(rowSums(x, na.rm = TRUE) == 0))
        warning("you have empty rows: their dissimilarities may be meaningless in method ",
                dQuote(inm))
    if (!method %in% c(1,2,6,16) && any(x < 0, na.rm = TRUE))
        warning("results may be meaningless because data have negative entries in method ",
                dQuote(inm))
    if (method == 11 && any(colSums(x) == 0)) 
        warning("data have empty species which influence the results in method ",
                dQuote(inm))
    if (method == 6) # gower, but no altGower
        x <- decostand(x, "range", 2, na.rm = TRUE, ...)
    if (method == 16) # mahalanobis
        x <- veganMahatrans(scale(x, scale = FALSE))
    if (binary) 
        x <- decostand(x, "pa")
    tempLabels <- dimnames(x)[[1]]
    #x <- as.matrix(x)
    N <- nrow(x)
    #print("After as matrix x")
    #print(gc())
    #y <- as.matrix(y)
    #print("After as matrix y")
    #print(gc())
    nc_ <- ncol(x)
    x_ <- as.double(x)
    x <- NULL
    print("After as double x")
    print(gc())
    gc()
    y_ <- as.double(y)
    y <- NULL
    print("After as double y")
    print(gc())
    #readline(prompt="Press [enter] to continue")
    if (method %in% c(7, 13, 15) && !identical(all.equal(as.integer(x), 
                                                     as.vector(x)), TRUE)) 
        warning("results may be meaningless with non-integer data in method ",
                dQuote(inm))
    d <- .C("veg_distanceMasked", x = x_, y = y_, nr = N, nc = nc_, 
            d = double(N * (N - 1)/2), diag = as.integer(FALSE), 
            method = as.integer(method), NAOK = na.rm, PACKAGE = "vegan")$d
    if (method == 10) 
        d <- 2 * d/(1 + d)
    d[d < ZAP] <- 0
    if (any(is.na(d))) 
        warning("missing values in results")
    attr(d, "Size") <- N
    attr(d, "Labels") <- tempLabels # dimnames(x)[[1]]
    attr(d, "Diag") <- diag
    attr(d, "Upper") <- upper
    attr(d, "method") <- paste(if (binary) 
                               "binary ", METHODS[method], sep = "")
    attr(d, "call") <- match.call()
    class(d) <- "dist"
    d
}
