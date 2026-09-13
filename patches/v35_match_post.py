#!/usr/bin/env python3
"""Route contribution/discipline completion through the v35 archive enrichers."""
import os,struct,sys,tempfile,zipfile
def cp_end(d):
 p=8;c=struct.unpack_from('>H',d,p)[0];p+=2;i=1
 while i<c:
  t=d[p];p+=1
  if t==1:n=struct.unpack_from('>H',d,p)[0];p+=2+n
  elif t in (3,4):p+=4
  elif t in (5,6):p+=8;i+=1
  elif t in (7,8,16,19,20):p+=2
  elif t in (9,10,11,12,17,18):p+=4
  elif t==15:p+=3
  else:raise RuntimeError('cp tag %d'%t)
  i+=1
 return c,p
def add(d,name):
 c,end=cp_end(d);e=[]
 def u(s):b=s.encode();e.append(b'\x01'+struct.pack('>H',len(b))+b);return c+len(e)-1
 ou=u('pfmMatchPost35');oc=c+len(e);e.append(b'\x07'+struct.pack('>H',ou));nu=u(name);du=u('()V');nt=c+len(e);e.append(b'\x0c'+struct.pack('>HH',nu,du));ref=c+len(e);e.append(b'\x0a'+struct.pack('>HH',oc,nt));return d[:8]+struct.pack('>H',c+len(e))+d[10:end]+b''.join(e)+d[end:],ref
def patch(d):
 d,cr=add(d,'runContrib');d,dr=add(d,'runDiscipline');a=b'\xb8\x00\x2b';b=b'\xb8\x00\x30'
 if d.count(a)!=1 or d.count(b)!=1:raise RuntimeError('unexpected calls %d/%d'%(d.count(a),d.count(b)))
 return d.replace(a,b'\xb8'+struct.pack('>H',cr),1).replace(b,b'\xb8'+struct.pack('>H',dr),1)
def main(path):
 with zipfile.ZipFile(path) as z:es=[(x,z.read(x.filename)) for x in z.infolist()]
 fd,tmp=tempfile.mkstemp(prefix='pfm-v35-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd)
 try:
  with zipfile.ZipFile(tmp,'w') as out:
   for x,d in es:out.writestr(x,patch(d) if x.filename=='pfmAvailable70.class' else d)
  os.replace(tmp,path)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
 print('v35 post-match: assists and discipline archived after stock ledgers')
if __name__=='__main__':main(sys.argv[1])
