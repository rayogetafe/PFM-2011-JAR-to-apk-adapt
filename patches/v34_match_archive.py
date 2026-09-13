#!/usr/bin/env python3
"""Route both user and AI fixture finalizers through the persistent v34 archive."""
import os,struct,sys,tempfile,zipfile

def cp_end(data):
 p=8;count=struct.unpack_from('>H',data,p)[0];p+=2;i=1
 while i<count:
  tag=data[p];p+=1
  if tag==1:n=struct.unpack_from('>H',data,p)[0];p+=2+n
  elif tag in (3,4):p+=4
  elif tag in (5,6):p+=8;i+=1
  elif tag in (7,8,16,19,20):p+=2
  elif tag in (9,10,11,12,17,18):p+=4
  elif tag==15:p+=3
  else:raise RuntimeError('unsupported cp tag %d'%tag)
  i+=1
 return count,p

def add_ref(data):
 count,end=cp_end(data);e=[]
 def utf(s):
  b=s.encode();e.append(b'\x01'+struct.pack('>H',len(b))+b);return count+len(e)-1
 ou=utf('pfmLeagueMatchArchive34');oc=count+len(e);e.append(b'\x07'+struct.pack('>H',ou));nu=utf('finish');du=utf('(Ldw;Ldw;[Ljava/util/Vector;)V');nt=count+len(e);e.append(b'\x0c'+struct.pack('>HH',nu,du));ref=count+len(e);e.append(b'\x0a'+struct.pack('>HH',oc,nt))
 return data[:8]+struct.pack('>H',count+len(e))+data[10:end]+b''.join(e)+data[end:],ref

def patch(data):
 data,ref=add_ref(data);old=b'\xb8\x02\x49'
 if data.count(old)!=2:raise RuntimeError('expected two ca result finalizer calls, got %d'%data.count(old))
 return data.replace(old,b'\xb8'+struct.pack('>H',ref))

def main(path):
 with zipfile.ZipFile(path) as z:entries=[(x,z.read(x.filename)) for x in z.infolist()]
 fd,tmp=tempfile.mkstemp(prefix='pfm-v34-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);seen=False
 try:
  with zipfile.ZipFile(tmp,'w') as out:
   for item,content in entries:
    if item.filename=='ca.class':content=patch(content);seen=True
    out.writestr(item,content)
  if not seen:raise RuntimeError('ca.class missing')
  os.replace(tmp,path)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
 print('v34 match archive: captured exact fixture lineups, goals and minutes')

if __name__=='__main__':
 if len(sys.argv)!=2:raise SystemExit('usage: v34_match_archive.py CORE_JAR')
 main(sys.argv[1])
