"""Approximate Latin spans from actual CTC emissions; no known menu strings."""
import math
import re
import cv2
import numpy as np

def infer(session,crop):
    h,w=crop.shape[:2]; width=math.ceil(w*48/h); total=max(320,width)
    image=np.zeros((3,48,total),np.float32)
    image[:,:,:width]=cv2.resize(crop,(width,48)).transpose(2,0,1)/127.5-1
    return session.run(None,{'x':image[None]})[0][0], total*h/48

def aligned(values,chars):
    labels=values.argmax(1); tokens=[]; previous=0
    for t,index in enumerate(labels):
        if index and index!=previous:tokens.append({'text':chars[index],'first':t,'last':t})
        elif index and tokens:tokens[-1]['last']=t
        previous=index
    return tokens

def spans(values,chars,full_width,crop_width):
    tokens=aligned(values,chars); text=''.join(t['text'] for t in tokens)
    assert all(len(t['text'])==1 for t in tokens)
    centers=[(t['first']+t['last']+1)/2 for t in tokens]
    unit=full_width/len(values)
    for match in re.finditer(r'[A-Za-z0-9][A-Za-z0-9 .%/+_()−-]*',text):
        if not re.search('[A-Za-z]',match.group()) or len(match.group().strip())<2:continue
        a,b=match.span(); left=0 if a==0 else (centers[a-1]+centers[a])/2*unit
        right=crop_width if b==len(tokens) else (centers[b-1]+centers[b])/2*unit
        left=max(0,min(crop_width-1,math.floor(left))); right=max(left+1,min(crop_width,math.ceil(right)))
        yield {'old':match.group(),'range':[a,b],'x':[left,right],'ctcText':text}
