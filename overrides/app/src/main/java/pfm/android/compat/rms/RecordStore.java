package pfm.android.compat.rms;

import java.io.*;
import java.util.*;
import pfm.android.AndroidRuntime;

public final class RecordStore {
    private static final int MAGIC=0x50465231; // PFR1
    private final String name;
    private final ArrayList<byte[]> records = new ArrayList<>();
    private boolean closed;

    private RecordStore(String name) { this(name, true); }
    private RecordStore(String name, boolean load) { this.name=name; if(load) load(); }
    private static File dir() { File d=new File(AndroidRuntime.context().getFilesDir(),"rms"); if(!d.exists()) d.mkdirs(); return d; }
    private File file() { return new File(dir(), safe(name)+".rms"); }
    private static String safe(String s) { return s.replaceAll("[^A-Za-z0-9._-]","_"); }

    public static RecordStore openRecordStore(String name, boolean createIfNecessary) {
        File f=new File(dir(),safe(name)+".rms");
        if(!f.exists() && !createIfNecessary) throw new RuntimeException("RecordStore not found: "+name);
        return new RecordStore(name);
    }
    public static byte[] checkpoint(String name) throws IOException {
        File file = new RecordStore(name, false).file();
        if (!file.exists()) return null;
        long length = file.length();
        if (length > 4 * 1024 * 1024) throw new IOException("RMS checkpoint too large: " + name);
        byte[] data = new byte[(int) length];
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) { in.readFully(data); }
        return data;
    }

    public static void restoreCheckpoint(String name, byte[] data) throws IOException {
        if (data == null) {
            deleteRecordStore(name);
            return;
        }
        RecordStore store = new RecordStore(name, false);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            store.read(in, true);
            if (in.available() != 0) throw new IOException("Trailing RMS checkpoint bytes");
        }
        store.save();
    }

    public static void deleteRecordStore(String name) {
        File f=new File(dir(),safe(name)+".rms"); if(f.exists() && !f.delete()) throw new RuntimeException("Cannot delete "+name);
    }
    public synchronized int getNumRecords() { ensure(); int n=0; for(byte[] b:records) if(b!=null)n++; return n; }
    public synchronized byte[] getRecord(int id) { ensure(); byte[] b=at(id); return Arrays.copyOf(b,b.length); }
    public synchronized int addRecord(byte[] data,int offset,int numBytes) { ensure(); records.add(Arrays.copyOfRange(data,offset,offset+numBytes)); save(); return records.size(); }
    public synchronized void setRecord(int id,byte[] data,int offset,int numBytes) { ensure(); while(records.size()<id)records.add(null); records.set(id-1,Arrays.copyOfRange(data,offset,offset+numBytes)); save(); }
    public synchronized RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated) {
        ensure(); ArrayList<Integer> ids=new ArrayList<>();
        for(int i=0;i<records.size();i++){ byte[] r=records.get(i); if(r!=null && (filter==null||filter.matches(Arrays.copyOf(r,r.length)))) ids.add(i+1); }
        if(comparator!=null) ids.sort((a,b)->comparator.compare(records.get(a-1),records.get(b-1)));
        final int[] a=new int[ids.size()]; for(int i=0;i<a.length;i++)a[i]=ids.get(i);
        return new RecordEnumeration(){ int p; public boolean hasNextElement(){return p<a.length;} public int nextRecordId(){if(p>=a.length)throw new NoSuchElementException();return a[p++];} };
    }
    public synchronized void closeRecordStore(){ if(!closed){ save(); closed=true; } }
    private byte[] at(int id){ if(id<1||id>records.size()||records.get(id-1)==null)throw new RuntimeException("Bad record id "+id);return records.get(id-1); }
    private void ensure(){ if(closed)throw new IllegalStateException("RecordStore closed"); }
    private void load(){
        File f=file(); if(!f.exists())return;
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(f)))){
            read(in, false);
        }catch(IOException e){throw new RuntimeException("Cannot load RMS "+name,e);}
    }
    private void read(DataInputStream in, boolean checkpoint) throws IOException {
        if (in.readInt() != MAGIC) throw new IOException("bad RMS magic");
        int count = in.readInt();
        if (checkpoint && (count < 0 || count > in.available() / 4)) throw new IOException("Invalid RMS record count");
        for (int i = 0; i < count; i++) {
            int length = in.readInt();
            if (checkpoint && (length < -1 || length > in.available())) throw new IOException("Invalid RMS record length");
            if (length < 0) records.add(null);
            else {
                byte[] record = new byte[length];
                in.readFully(record);
                records.add(record);
            }
        }
    }
    private void save(){
        File f=file(), tmp=new File(f.getPath()+".tmp");
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))){
            out.writeInt(MAGIC); out.writeInt(records.size()); for(byte[] b:records){if(b==null)out.writeInt(-1);else{out.writeInt(b.length);out.write(b);}}
        }catch(IOException e){throw new RuntimeException("Cannot save RMS "+name,e);}
        if(f.exists()&&!f.delete())throw new RuntimeException("Cannot replace RMS "+name);
        if(!tmp.renameTo(f))throw new RuntimeException("Cannot commit RMS "+name);
    }
}
