package icedev.io;

import java.io.*;
import java.util.*;

import icedev.io.ISO9660.*;
import icedev.io.ISO9660.SectorInputStream;

public class IsoArchive implements Closeable {
	public class IsoEntry {
		public final String name;
		protected final int sector;
		public final long length;
		
		public IsoEntry(String name, int sector, long length) {
			this.name = name;
			this.sector = sector;
			this.length = length;
		}

		public LittleEndianInputStream open(int offset) throws IOException {
			return new LittleEndianInputStream(IsoArchive.this.open(this, offset));
		}
		
		public LittleEndianInputStream open() throws IOException {
			return new LittleEndianInputStream(IsoArchive.this.open(this));
		}
		
	}
	
	Map<String, IsoEntry> entries = new HashMap<>();
	private ISO9660 iso;

	public Set<String> keys() {
		return entries.keySet();
	}
	
	public IsoArchive(File isoFile, boolean rawMode) throws IOException {
		iso = new ISO9660(new RandomAccessFile(isoFile, "r"), rawMode? 2352 : 0);
		
		populateEntries();
	}
	
	public IsoEntry get(String path) {
		return entries.get(path);
	}
	
	public InputStream open(String path) throws IOException {
		return open(get(path));
	}
	
	public InputStream open(IsoEntry entry) throws IOException {
		return iso.getStreamAtSector(entry.sector); //new IsoEntryInputStream(entry.offset, entry.offset+entry.length);
	}
	
	public InputStream open(IsoEntry entry, int offset) throws IOException {
		int blocks = offset / iso.blockSize;
		int skip = offset % iso.blockSize;
		
		SectorInputStream stream = iso.getStreamAtSector(entry.sector + blocks);
		stream.skip(skip);
		return stream;
	}
	
	private void populateEntries() throws IOException {
		iso.readVolumeDescriptor();
		List<PathEntry> table = iso.readPathTable();
		List<DirEntry> dirs = new ArrayList<>();
		for(PathEntry p : table) {
			iso.readDirEntries(p, dirs);
			for(DirEntry d : dirs) {
				if(d.isDirectory())
					continue;
				IsoEntry e = new IsoEntry(d.fileIdentifier.split(";")[0], d.locationOfExtent, d.dataLength);
				entries.put(p.path + "/" + e.name, e);
			}
			dirs.clear();
		}
	}
	
	static class LimitedInputStream extends FilterInputStream {
		private int left;

		protected LimitedInputStream(InputStream in, int limit) {
			super(in);
			this.left = limit;
		}
		
		@Override
		public int read() throws IOException {
			if(left <= 0)
				return -1;
			left--;
			return in.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if(left <= 0)
				return -1;
			int read = in.read(b, off, Math.min(len, left));
			if(read > 0)
				left -= read;
			return read;
		}
		
		@Override
		public long skip(long n) throws IOException {
			long skipped = in.skip(Math.min(n, left));
			left -= skipped;
			return skipped;
		}
		
		@Override
		public int available() throws IOException {
			int available = super.available();
			return Math.min(available, left);
		}
	}
	
	public void close() throws IOException {
		iso.raf.close();
	}
}
