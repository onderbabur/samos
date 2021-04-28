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

cluster <- function(vsmFile, nameFile, outputFolder) {
# load vsm file
vsmMatrix <- read.csv(vsmFile, header=FALSE)
names <- read.csv(nameFile, header=FALSE)
names$V1 <- as.character(names$V1)
rownames(vsmMatrix) <- names$V1

# calculate cosine distance matrix
dist <- as.dist(1 - coop::cosine(as.matrix(t(as.matrix(vsmMatrix)))))

# hierarchical clustering
hc <- hclust(dist)
hc$labels <- names$V1

# regular cut at threshold = 0.8
ct <- cutree(hc, h = 0.8)

# turn into data frame
df <- as.data.frame(ct)

# add row names(indices) as the second column
df[,2] <- names$V1 # original names

# order
df <- df[order(df[1]),]
rownames(df) <- NULL

colnames(df) <- c("clusterLabel", "model")

# write cluster labels
write.csv(df, paste0(outputFolder, "/clusterLabels.csv"), row.names = FALSE)

pdf(file = paste0(outputFolder, "/clusterPlot.pdf"),   # The directory you want to save the file in
#    width = 4, # The width of the plot in inches
#    height = 4
    ) # The height of the plot in inches

plot(hc)
# Step 3: Run dev.off() to create the file!
dev.off()

}

#a <- cluster("asd", "qwe", "we")
