#
# Copyright (c) 2015-2022 Onder Babur
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

detectClones <- function(vsmFile, vsmMaskFile, nameFile, sizeFile, outputFolder) {

# load vsm files
vsmMatrix <- read.big.matrix(vsmFile, header=FALSE, type='double')
vsmMask <- read.big.matrix(vsmMaskFile, header=FALSE, type='double')
names <- read.csv(nameFile, header=FALSE)  
sizes <- read.csv(sizeFile, header=FALSE) 

distMatrix <- maskedBrayCurtis(vsmMatrix, vsmMask, sizes[,1]);

# cut
#ct1.1 <- cutree(hc1, h=0.00)
#ct1.2 <- cutree(hc1, h=0.10)
#ct1.3 <- cutree(hc1, h=0.30)

clones1 <- getClones(as.dist(distMatrix), 0.00, names, sizes)
clones2 <- getClones(as.dist(distMatrix), 0.10, names, sizes)
clones3 <- getClones(as.dist(distMatrix), 0.30, names, sizes)

# for exclusively type b-c clones, TODO debug
#clones2_subset <- subset(clones2, !(clones2$x %in% clones1$x))
#clones3_subset <- subset(clones3, !(clones3$x %in% clones2$x) & !(clones3$x %in% clones1$x))

write.csv(clones1, paste0(outputFolder, "/cloneClustersTypeA.csv"), row.names = FALSE)
write.csv(clones2, paste0(outputFolder, "/cloneClustersTypeB.csv"), row.names = FALSE)
write.csv(clones3, paste0(outputFolder, "/cloneClustersTypeC.csv"), row.names = FALSE)
}

maskedBrayCurtis <- function(data, dataFix, sizeVector){
  # special case when dimension is 1, matrix is erroneously loaded as transposed
  if (dim(data)[2] == 1 & dim(dataFix)[2] == 1) {
    data <- t(t(as.matrix(data)))
    dataFix <- t(t(as.matrix(dataFix)))
  }
  size <- dim(data)[1]
  distanceMatrix <- matrix(0,nrow = size, ncol = size)
  #baseManhattanMatrix <- as.matrix(stats::dist(data, method="manhattan"))
  #maxDistance <- max(as.vector(baseManhattanMatrix))
  
  #setDT(dataFix)
  dataMatrix <- as.matrix(data)
  dataMatrixFix <- as.matrix(dataFix)
  
  whVector <- vector("list", size)
  for (i in c(1:size)) {
    # print(i)
    xtime <- Sys.time()
    whVector[[i]] <- which(dataMatrixFix[i,] >= 1)
    # print(Sys.time() - xtime)
  }
  lengthVector <- vector("double", size)
  for (i in c(1:size))
    lengthVector[i] <- length(whVector[[i]])
  for (i in c(1:size)) 
    distanceMatrix[i,i] = 0
  for (i in c(1:(size-1))){
    # print(i)
    lhs <- dataMatrix[i,]
    lhsFix <- dataMatrixFix[i,]
    #lwh <- which(lhsFix >= 1)
    lwh <- whVector[[i]]
    #lengthLHS <- length(lwh)
    lengthLHS <- lengthVector[i]
    #start.time <- Sys.time()
    for(j in c((i+1):size)){
      sizeTotal <- sizeVector[i] + sizeVector[j]
      if (sizeTotal > 0 & lengthLHS > 0){
        #distanceMatrix[i,j] <- baseManhattanMatrix[i,j] / (sizeVector[i] + sizeVector[j])
        #distanceMatrix[i,j] <- baseManhattanMatrix[i,j] / max(max(baseManhattanMatrix[i,]), max(baseManhattanMatrix[j,]))
        #rhs <- test1[j,]
        #rhsFix <- test1Fix[j,]
        #wh <- which(lhs == rhs & (lhs < 1 & rhs < 1))
        #rwh <- which(rhsFix >= 1)
        #rwh <- whVector[[j]]
        #lengthRHS <- length(rwh)
        #lengthRHS <- lengthVector[j]
        if (lengthVector[j] == 0) {
          distanceMatrix[i,j] <- 1
          distanceMatrix[j,i] <- 1
        }
        else {
          #xtime <- Sys.time()
          #miniVSM <- rbind(lhs, rhs)[,c(lwh, rwh)]
          #miniVSM <- test1[c(i,j),c(lwh,rwh)]
          #print(Sys.time() - xtime)
          #xtime <- Sys.time()
          #print(paste(i, j, ""))
          distanceMatrix[i,j] <- vegdist(dataMatrix[c(i,j),c(lwh,whVector[[j]])], method="bray")
          #print(Sys.time() - xtime)
          distanceMatrix[j,i] <- distanceMatrix[i,j]
        }
        #print(j) #print(distanceMatrix[i,j])
      }
      else {
        distanceMatrix[i,j] <- 1
        distanceMatrix[j,i] <- 1
      }
    }
    #print(Sys.time() - start.time)
  }
  return(distanceMatrix);
} 

getClones <- function(distObj, threshold, names, sizes){
  
  # cut
  #ct1.1 <- cutree(hc1, h=cutThreshold)
  db <- dbscan(distObj, eps = threshold, minPts = 2)
  
  # set the main labels 
  #ct <- ct1.1
  ct <- db$cluster
  
  if (sum(ct) == 0) return(NULL);
  
  # turn into data frame
  df <- as.data.frame(ct)
  
  # add row names(indices) as the second column
  df[,2] <- rownames(df) # original names
  
  # take subset w.r.t cluster labels and outliers
  nonSingletonClusters <- which(table(ct) > 1)
  nonSingletonMask <- is.element(df$ct, as.numeric(names(nonSingletonClusters)))
  nonOutlierClusters <- which(ct > 0)
  nonOutlierMask <- is.element(df$V2, nonOutlierClusters)
  df_clusters <- subset(df, nonSingletonMask & nonOutlierMask)
  
  # order w.r.t. cluster label
  df_clusters_ordered <- df_clusters[order(df_clusters[1]),]
  
  # df_clusters_ordered_colSums <- df_clusters_ordered
  #rs <- rowSums(testBase) # instead of test1, to get exactly the sizes
  rs <- sizes[,1]
  #f_rs <- function(x) {return(rs[df_clusters_ordered[as.character(x),1]]);}
  f_rs <- function(x) {return(rs[as.integer(x)]);}
  df_clusters_ordered_rowSums <- df_clusters_ordered
  df_clusters_ordered_rowSums[,3] <- sapply(df_clusters_ordered[,2], f_rs)
  
  # optional when want to write names instead of indices
  #names <- as.matrix(read.csv("/Users/obabur/Dropbox/MMP/papers/2017.08 Model Clone Detection/SCITEPRESS_Conference_Latex/figs/names.csv", header = FALSE))
  #df_clusters_ordered_rowSums[,2] <- sapply(df_clusters_ordered_rowSums[,2], function(x) names[1,as.integer(x)])
  df_clusters_ordered_rowSums[,2] <- sapply(df_clusters_ordered_rowSums[,2], function(x) names[as.integer(x),1]) # ->this is the line to turn on
  
  # aggregate
  #agr <- aggregate(df_clusters_ordered$"V2", by=list(ClusterLabel=df_clusters_ordered$"ct"), FUN=paste)
  agr1 <- aggregate(df_clusters_ordered_rowSums$"V2", by=list(ClusterLabel=df_clusters_ordered_rowSums$"ct"), FUN=paste1)
  agr2 <- aggregate(df_clusters_ordered_rowSums$"V3", by=list(ClusterLabel=df_clusters_ordered_rowSums$"ct"), FUN=mean)
  
  agr3 <- agr1
  agr3[,3] <- agr2[,2]
  
  agr3_ordered <- agr3[order(-agr3[3]),]
  
  colnames(agr3_ordered) <- c("clusterLabel", "models", "averageSize")
  
  return(agr3_ordered)
}

paste1 <- function(x){return(paste0(x,sep=" ", collapse=""));}
