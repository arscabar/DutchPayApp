"""Exploratory cross-engine policy. All output strings were actually recognized."""
import re
from menu_select import key

def numeric(s):return re.sub(r'[^0-9+.,/%×~−-]','',s)

def insertion(old,new):
    a=re.sub(r'\s','',old); b=re.sub(r'\s','',new); i=0; added=[]
    for char in b:
        if i<len(a) and char==a[i]:i+=1
        else:added.append(char)
    return i==len(a) and 1<=len(added)<=2 and all('가'<=c<='힣' for c in added)

def choose(row,extra_scales=False,protect=True,threshold=.88,multiple=True,allow_insert=False,minimal=False):
    old=row['old']; paddle=row['paddle']; ml=row['ml']
    # Otsu and aggressive 60/70% crops are independently tested, not name evidence.
    paddle=[v for v in paddle if v['mode'] not in ['otsu','center60','center70','aspect0.6','aspect1.4']]
    original=[v for v in paddle if v['mode'] in ['sdk','quad','android_base','original'] and key(v['text'])==key(old)]
    if protect and any(v['score']>=.97 for v in original):
        if allow_insert:
            for view in ml:
                if view['scale']==1 and insertion(old,view['text']) and numeric(old)==numeric(view['text']):
                    if any(v['mode']=='center80' and key(v['text'])==key(view['text']) and v['score']>=.9 for v in paddle):
                        return view['text'],'cross-engine Hangul insertion only'
        return old,'high-confidence original'
    if minimal:paddle=[v for v in paddle if v['mode'] in ['android_base','center80']]
    for view in sorted(ml,key=lambda v:abs(v['scale']-1)):
        if not extra_scales and view['scale']!=1:continue
        text=view['text']; target=key(text)
        if target==key(old):continue
        if not re.search('[가-힣]',text) or numeric(old)!=numeric(text):continue
        if not max(2,len(key(old))*.7)<=len(target)<=len(key(old))*1.3+2:continue
        same=[v for v in paddle if key(v['text'])==target and v['score']>=threshold]
        if not same:continue
        ml_votes=sum(key(v['text'])==target for v in ml)
        # Need two pixel variants, with at least one observation from each engine.
        supported=not multiple or len(same)>=2 or (extra_scales and ml_votes>=2)
        if not supported:continue
        return text,'cross-engine agreement'
    return old,'retained'
