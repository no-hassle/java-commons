package com.comoyo.emjar;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public class ZipScanner
{
    static final int METHOD_STORED = 0;

    static final int ENDCUR = 4;
    static final int ENDSTA = 6;

    static final long ZIP64_ENDSIG = 0x06064b50L;  // "PK\006\006"
    static final long ZIP64_LOCSIG = 0x07064b50L;  // "PK\006\007"
    static final int  ZIP64_ENDHDR = 56;           // ZIP64 end header size
    static final int  ZIP64_LOCHDR = 20;           // ZIP64 end loc header size
    static final int  ZIP64_EXTHDR = 24;           // EXT header size
    static final int  ZIP64_EXTID  = 0x0001;       // Extra field Zip64 header ID

    static final int  ZIP64_ENDLEN = 4;       // size of zip64 end of central dir
    static final int  ZIP64_ENDVEM = 12;      // version made by
    static final int  ZIP64_ENDVER = 14;      // version needed to extract
    static final int  ZIP64_ENDNMD = 16;      // number of this disk
    static final int  ZIP64_ENDDSK = 20;      // disk number of start
    static final int  ZIP64_ENDTOD = 24;      // total number of entries on this disk
    static final int  ZIP64_ENDTOT = 32;      // total number of entries
    static final int  ZIP64_ENDSIZ = 40;      // central directory size in bytes
    static final int  ZIP64_ENDOFF = 48;      // offset of first CEN header
    static final int  ZIP64_ENDEXT = 56;      // zip64 extensible data sector

    static final int  ZIP64_LOCDSK = 4;       // disk number start
    static final int  ZIP64_LOCOFF = 8;       // offset of zip64 end
    static final int  ZIP64_LOCTOT = 16;      // total number of disks


    private final File file;
    private final Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> nestedDescriptors;

    public ZipScanner(File file)
    {
        this.file = file;
        nestedDescriptors = new HashMap<>();
    }

    public Map<String, Map<String, OndemandEmbeddedJar.Descriptor>> scan()
        throws IOException
    {
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        final FileChannel chan = raf.getChannel();
        if (raf.length() <= Integer.MAX_VALUE) {
            final MappedByteBuffer map
                = chan.map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
            recurse(map, null);
        }
        raf.close();
        return nestedDescriptors;
    }

    public void recurse(
        final ByteBuffer map,
        final Map<String, OndemandEmbeddedJar.Descriptor> context)
        throws IOException
    {
        final ByteBuffer loc64 = findEocd(map, ZIP64_LOCSIG, ZIP64_LOCHDR);
        if (loc64 != null) {
            loc64.order(ByteOrder.LITTLE_ENDIAN);
            final int pos = loc64.position();
            final int locDsk = loc64.getInt(pos + ZIP64_LOCDSK);
            final long locOff = loc64.getLong(pos + ZIP64_LOCOFF);
            final int locTot = loc64.getInt(pos + ZIP64_LOCTOT);
            if (locDsk != 0 || locTot != 1) {
                throw new IOException("Split archives not supported");
            }
            if (locOff > Integer.MAX_VALUE) {
                throw new IOException("Unexpected oversize offset value");
            }
            map.position((int) locOff);
            final ByteBuffer eocd64 = map.slice();
            eocd64.order(ByteOrder.LITTLE_ENDIAN);
            final int eocdSig = eocd64.getInt(0);
            if (eocdSig != ZIP64_ENDSIG) {
                throw new IOException("Zip64 EOCD locator did not point to EOCD structure");
            }
            final int endNmd = eocd64.getInt(ZIP64_ENDNMD);
            final int endDsk = eocd64.getInt(ZIP64_ENDDSK);
            final long endTod = eocd64.getLong(ZIP64_ENDTOD);
            final long endTot = eocd64.getLong(ZIP64_ENDTOT);
            final long endSiz = eocd64.getLong(ZIP64_ENDSIZ);
            final long endOff = eocd64.getLong(ZIP64_ENDOFF);
            if (endNmd != 0 || endDsk != 0 || endTod != endTot) {
                throw new IOException("Split archives not supported");
            }
            if (endOff > Integer.MAX_VALUE) {
                throw new IOException("Unexpected oversize offset value");
            }
            parseDirectory(map, (int) endOff, (int) endSiz, context);
            return;
        }
        final ByteBuffer eocd = findEocd(map, ZipFile.ENDSIG, ZipFile.ENDHDR);
        if (eocd != null) {
            eocd.order(ByteOrder.LITTLE_ENDIAN);
            final int pos = eocd.position();
            final int curDiskNum = eocd.getShort(pos + ENDCUR);
            final int cdStartDisk = eocd.getShort(pos + ENDSTA);
            final int cdRecsHere = eocd.getShort(pos + ZipFile.ENDSUB);
            final int cdRecsTotal = eocd.getShort(pos + ZipFile.ENDTOT);
            final int cdSize = eocd.getInt(pos + ZipFile.ENDSIZ);
            final int cdOffs = eocd.getInt(pos + ZipFile.ENDOFF);
            if (curDiskNum != 0 || cdStartDisk != 0 || cdRecsHere != cdRecsTotal) {
                throw new IOException("Split archives not supported");
            }
            parseDirectory(map, cdOffs, cdSize, context);
            return;
        }
        throw new IOException("EOCD signature not found");
    }

    private void parseDirectory(
        final ByteBuffer map,
        final int offset,
        final int size,
        final Map<String, OndemandEmbeddedJar.Descriptor> context)
        throws IOException
    {
        map.position(offset);
        final ByteBuffer dir = map.slice();
        dir.limit(size);
        dir.order(ByteOrder.LITTLE_ENDIAN);
        int pos = 0;
        byte[] buf = new byte[256];
        while (pos < size) {
            final int sig = dir.getInt(pos);
            if (sig != ZipFile.CENSIG) {
                break;
            }
            final int method = dir.getShort(pos + ZipFile.CENHOW);
            final int compressedSize = dir.getInt(pos + ZipFile.CENSIZ);
            final int originalSize = dir.getInt(pos + ZipFile.CENLEN);
            final int nameLen = dir.getShort(pos + ZipFile.CENNAM);
            final int extraLen = dir.getShort(pos + ZipFile.CENEXT);
            final int commentLen = dir.getShort(pos + ZipFile.CENCOM);
            final int startDiskNum = dir.getShort(pos + ZipFile.CENDSK);
            final int headerOffs = dir.getInt(pos + ZipFile.CENOFF);
            if (nameLen > buf.length) {
                buf = new byte[buf.length * 2];
            }
            dir.position(pos + ZipFile.CENHDR);
            dir.get(buf, 0, nameLen);
            final String name = new String(buf, 0, nameLen, StandardCharsets.UTF_8);
            pos += ZipFile.CENHDR + nameLen + extraLen + commentLen;

            if (startDiskNum != 0) {
                continue;
            }
            map.position(headerOffs);
            if (method == METHOD_STORED && name.endsWith(".jar")) {
                final Map<String, OndemandEmbeddedJar.Descriptor> descriptors
                    = new HashMap<>(16);
                parseFile(map.slice(), descriptors, compressedSize);
                nestedDescriptors.put(name, descriptors);
            }
            if (context != null) {
                context.put(name, new OndemandEmbeddedJar.Descriptor(name, map, headerOffs, originalSize));
            }
        }
    }

    private void parseFile(
        final ByteBuffer map,
        final Map<String, OndemandEmbeddedJar.Descriptor> context,
        final int compressedSize)
        throws IOException
    {
        map.order(ByteOrder.LITTLE_ENDIAN);
        final int pos = map.position();
        final int sig = map.getInt(pos);
        if (sig != ZipFile.LOCSIG) {
            return;
        }
        final int nameLen = map.getShort(pos + ZipFile.LOCNAM);
        final int extraLen = map.getShort(pos + ZipFile.LOCEXT);
        map.position(pos + ZipFile.LOCHDR + nameLen + extraLen);
        final ByteBuffer nested = map.slice();
        nested.limit(compressedSize);
        recurse(nested, context);
    }

    private ByteBuffer findEocd(
        final ByteBuffer map,
        final long eocdSig,
        final int eocdLen)
        throws IOException
    {
        map.order(ByteOrder.LITTLE_ENDIAN);
        final int length = map.limit();
        int eocdPos = length - eocdLen;
        while (eocdPos > 0) {
            final long sig = map.getInt(eocdPos);
            if (sig == eocdSig) {
                map.position(eocdPos);
                return map.slice();
            }
            eocdPos--;
            if (length - eocdPos + eocdLen > Short.MAX_VALUE) {
                break;
            }
        }
        return null;
    }
}
