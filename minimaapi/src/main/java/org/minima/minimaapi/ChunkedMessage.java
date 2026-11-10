package org.minima.minimaapi;

public class ChunkedMessage {

    String[] mChunks;

    public ChunkedMessage(int zSize){
        mChunks = new String[zSize];
    }

    public void setChunk(int zPos, String zChunk){
        mChunks[zPos] = zChunk;
    }

    public boolean isFinished(){
        int size = mChunks.length;
        for(int i=0;i<size;i++){
            if(mChunks[i] == null){
                return false;
            }
        }
        return true;
    }

    public String getFullMessage(){
        StringBuffer ret = new StringBuffer();
        int size = mChunks.length;
        for(int i=0;i<size;i++){
            ret.append(mChunks[i]);
        }
        return ret.toString();
    }
}
