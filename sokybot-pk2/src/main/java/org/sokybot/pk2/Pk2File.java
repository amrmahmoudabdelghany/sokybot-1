/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.pk2;

import static org.sokybot.pk2.Constants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.sokybot.pk2.exception.Pk2FileCorruptedException;

import org.sokybot.pk2.exception.Pk2IOException;
import org.sokybot.pk2.exception.Pk2InvalidKeyException;
import org.sokybot.security.Blowfish;
import org.sokybot.security.IBlowfish;

/**
 * @author AMROO
 */
@Slf4j
public class Pk2File implements IPk2Driver {

	private FileChannel channel;

	// private RandomAccessFile file;
	private final String filePath;
	private boolean isOpened = false;

	private IBlowfish blowfish;

	Pk2File(String pk2FilePath) {
		this.filePath = pk2FilePath;
	}

	// need to optimization

	void open() {
		if (this.channel == null || !this.channel.isOpen()) {

			if (!Files.exists(Paths.get(filePath))) {
				throw new IllegalArgumentException("Pk2 File " + filePath + " is no longer exists");
				// throw new Pk2FileNotFoundException();
			}

			try {
				this.channel = FileChannel.open(Paths.get(this.filePath), StandardOpenOption.READ);
				channel.position(0);

			} catch (IOException ex) {

				throw new Pk2IOException("Could not open channel with pk2 file " + filePath, filePath, ex);
			}

			Pk2Header header;
			try {
				header = readHeader(channel);
			} catch (IOException e) {
				throw new Pk2FileCorruptedException("Could not read the header of pk2 file : " + filePath, filePath, 0,
						e);

			}

			this.blowfish = Blowfish.newInstance(HEADER_VALIDATION_KEY);// new Blowfish();

			if (header.encrypted == 1) {

				if (!Arrays.equals(Arrays.copyOf(blowfish.encode(0, HEADER_VALIDATION_STATEMENT.getBytes()), 3),
						Arrays.copyOf(header.verify, 3))) {

					throw new Pk2InvalidKeyException("Pk2 file encrepted by unknonw key ", filePath,
							HEADER_VALIDATION_KEY, header.verify);

				}
			}

			// execution could continue even these attributes are varies from what expected
			if (!(header.name.trim().equalsIgnoreCase(HEADER_NAME))) {
				log.warn("Unexpected Pk2 File Name Attribute  , expect {} but it was {} ", HEADER_NAME, header.name);
			}

			if (!(header.version == HEADER_VERSION)) {
				log.warn("Unexpected Pk2 File Version Attribute !! , expect {} but it was {} ", HEADER_VERSION,
						header.version);
			}

			this.isOpened = true;
		}
	}

	@Override
	public List<JMXFile> find(String regex, int limit) {
		// Validate that file is opened before operations
		if (!isOpened || channel == null || blowfish == null) {
			throw new IllegalStateException("Pk2File is not opened. Call open() first.");
		}
		
		String effectiveRegex = resolvePattern(regex);

		List<JMXFile> res = new ArrayList<>();

		// openIfClosed() ;
		// log.debug("Channel Opened with file {} and arguments verified" ,
		// this.filePath);
		if (!effectiveRegex.isBlank()) {

			Stack<String> pathStack = getPathStack(effectiveRegex);
			
			// Validate pathStack is not empty
			if (pathStack.isEmpty()) {
				log.warn("Empty path stack from regex: {}", regex);
				return res;
			}

			String fileName = pathStack.remove(0);
			if (pathStack.isEmpty()) {
				res = findFile(fileName, FIRST_BLOCK_POS, limit);
			} else {
				res = find(pathStack, fileName, limit);
			}

		}
		return res;

	}

	private List<JMXFile> findFile(String regex, long pos, int limit) {
		
		List<JMXFile> res = new ArrayList<>();

		if (limit == 0)
			return res;

		List<JMXDirectory> jMXDirectories = new ArrayList<>();
		EntryBlock block = readBlock(pos);
		Iterator<Pk2Entry> ite = block.getIterator();

		while (ite.hasNext()) {
			Pk2Entry entry = ite.next();
			switch (entry.type) {
			case 0:
				break;
			case 1:

				if (!entry.name.contains(".") && !entry.name.contains("..")) {

					jMXDirectories.add(new JMXDirectory(entry.name, entry.position));
				}
				break;
			case 2:
				if (Pattern.matches(regex, entry.name)) {
					res.add(JMXFile.builder()
							.pkFilePath(this.filePath)
							.name(entry.name)
							.position(entry.position)
							.size(entry.size)
							.build());

				}

				if (limit > 0 && res.size() == limit) {
					return res;
				}

				break;

			}
		}

		long nextChain = block.getLastEntry().nextChain;

		if (nextChain > 0) {
			// Fix: Prevent negative limit calculation
			int remainingLimit = limit > 0 ? Math.max(0, limit - res.size()) : limit;
			List<JMXFile> nextFiles = findFile(regex, nextChain, remainingLimit);
			res.addAll(nextFiles);

			if (limit > 0 && res.size() == limit) {
				return res;
			}

		}

		for (JMXDirectory jMXDirectory : jMXDirectories) {
			// Fix: Prevent negative limit calculation
			int remainingLimit = limit > 0 ? Math.max(0, limit - res.size()) : limit;
			List<JMXFile> children = findFile(regex, jMXDirectory.getPosition(), remainingLimit);
			
			res.addAll(children);
			if (limit > 0 && res.size() == limit) {
				return res;
			}
		}

		return res;

	}

	private List<JMXFile> find(Stack<String> pathStack, String regex, int limit) {

		List<JMXFile> res = new ArrayList<>();

		Stack<Long> posStack = new Stack<>();
		posStack.push(FIRST_BLOCK_POS);

		while (!pathStack.isEmpty()) {
			String targetDir = pathStack.pop();

			List<Pk2Entry> dirs = new ArrayList<>();

			while (!posStack.isEmpty()) {
				dirs.addAll(findDirEntry(posStack.pop(), targetDir));

			}
			dirs.forEach(pos -> posStack.push(pos.position));

		}

		// (limit > 0 && res.size() < limit) || limit <= 0)
		while (!posStack.isEmpty() && (limit <= 0 || res.size() < limit)) {
			// Fix: Calculate remaining limit to prevent negative values
			int remainingLimit = limit > 0 ? Math.max(0, limit - res.size()) : limit;
			res.addAll(findFile(regex, posStack.pop(), remainingLimit));

		}

		return res;

	}

	private List<Pk2Entry> findDirEntry(long pos, String regex) {
		//log.debug("find dir entry {} " , regex);
		EntryBlock block = readBlock(pos);
		Iterator<Pk2Entry> ite = block.getIterator();
		List<Pk2Entry> res = new ArrayList<>();

		while (ite.hasNext()) {
			Pk2Entry entry = ite.next();

			if (entry.type == 1) {
				if (!entry.name.contains(".") && !entry.name.contains("..")) {

					if (Pattern.matches(regex, entry.name)) {
					//	log.debug("dir {} "  ,  entry.name);
						res.add(entry);
					}

				}

			}
		}
		long nextChain = block.getLastEntry().nextChain;
		if (nextChain > 0) {

			res.addAll(findDirEntry(nextChain, regex)); // getFile(nextChain, fileName);

		}

		return res;

	}

	Stack<String> getPathStack(String path) {

		Stack<String> pathStack = new Stack<>();
		Stack<String> nestedFlag = new Stack<>();

		String pathComp = "";
		char c;
		for (int i = path.length() - 1; i >= 0; i--) {
			c = path.charAt(i);
			switch (c) {
			case '/':

				if (nestedFlag.isEmpty()) {

					if (!pathComp.isBlank()) {
						pathStack.push(pathComp);
						pathComp = "";
					}
				} else {
					pathComp = c + pathComp;
				}
				break;
			case ')':
				nestedFlag.push(c + "");
				pathComp = c + pathComp;
				break;
			case '(':
				nestedFlag.pop();
				pathComp = c + pathComp;
				break;
			default:
				pathComp = c + pathComp;
			}
		}
		if (!pathComp.isBlank()) {
			pathStack.push(pathComp);
		}

		return pathStack;

	}

	private Pk2Header readHeader(FileChannel pk2FileChannel) throws IOException {
		Pk2Header res = new Pk2Header();
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		int totalReadBytes = pk2FileChannel.read(buffer);
		
		// Validate that we read the complete header
		if (totalReadBytes < HEADER_SIZE) {
			throw new IOException(String.format(
				"PK2 file header incomplete. Expected %d bytes, read %d bytes. File may be corrupted or truncated.",
				HEADER_SIZE, totalReadBytes));
		}
		
		buffer.position(0);
		byte[] nameBuffer = new byte[30];
		buffer.get(nameBuffer);
		res.name = new String(nameBuffer);
		res.version = buffer.getInt();
		res.encrypted = buffer.get();

		res.verify = new byte[16];
		buffer.get(res.verify);

		return res;

	}

	private JMXFile getFile(long pos, String fileName) {
		List<JMXDirectory> jMXDirectories = new ArrayList<>();
		EntryBlock block = readBlock(pos);
		Iterator<Pk2Entry> ite = block.getIterator();
		while (ite.hasNext()) {
			Pk2Entry entry = ite.next();
			switch (entry.type) {
			case 0:
				break;
			case 1:
				if (!entry.name.contains(".") && !entry.name.contains("..")) {

					jMXDirectories.add(new JMXDirectory(entry.name, entry.position));
				}
				break;
			case 2:
				if (entry.name.equalsIgnoreCase(fileName)) {
					return JMXFile.builder().pkFilePath(this.filePath).name(entry.name).position(entry.position)
							.size(entry.size).build();
				}
				break;
			}
		}
		long nextChain = block.getLastEntry().nextChain;
		if (nextChain > 0) {
			JMXFile target = getFile(nextChain, fileName);
			if (target != null) {
				return target;
			}
		}

		for (JMXDirectory jMXDirectory : jMXDirectories) {
			JMXFile target = getFile(jMXDirectory, fileName);
			if (target != null) {
				return target;

			}
		}
		return null;
	}

	private JMXFile getFile(JMXDirectory parent, String fileName) {
		return getFile(parent.getPosition(), fileName);
	}

	// this.channel = null;
//		this.blowfish = null;
//		this.reader = null;

	@Override
	public void close() throws IOException {
		if (this.isOpened) {
			if (this.channel != null) {
				this.channel.close();
			}
			this.channel = null;
			this.blowfish = null;
			this.isOpened = false;
		}
	}

	private EntryBlock readBlock(long pos) {
		
		long startLocation = pos;

		Pk2Entry[] block = new Pk2Entry[20];
		for (int i = 0; i < 20; i++) {
			block[i] = readEntry(startLocation);
			startLocation += 128;
		}
		return new EntryBlock(block);
	}

	private Pk2Entry readEntry(long pos) {
		ByteBuffer buffer = ByteBuffer.allocate(128);
		Pk2Entry entry = new Pk2Entry();

		try {
			channel.position(pos);
			channel.read(buffer);
			byte[] decode = buffer.array();
			blowfish.decode(0, decode);
			buffer = ByteBuffer.wrap(decode);
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			// reader.setBuffer(buffer);

			entry.type = buffer.get();

			byte[] nameBuffer = new byte[81];
			buffer.get(nameBuffer);
			entry.name = new String(nameBuffer).trim();

			entry.accessTime = buffer.getLong();
			entry.createTime = buffer.getLong();
			entry.modifyTime = buffer.getLong();
			entry.position = buffer.getLong();
			entry.size = buffer.getInt();
			entry.nextChain = buffer.getLong();

			entry.padding = new byte[2];
			buffer.get(entry.padding);

		} catch (IOException ex) {
			throw new Pk2FileCorruptedException(
					"The reprsented pk2 file is misformatted  , could not read pk2 entry at position " + pos, filePath,
					pos, ex);

		}
		return entry;

	}

	private class Pk2Header {
		private String name;
		private int version;
		private byte encrypted;
		private byte[] verify;
		private byte[] reserved;
	}

	private class Pk2Entry {
		private byte type; // 1 JMXDirectory , 2 JMXFile , 0 null
		private String name;
		private long accessTime, createTime, modifyTime;
		private long position, nextChain;
		private int size;
		private byte[] padding;
	}

	private class EntryBlock {
		private final Pk2Entry[] block;

		EntryBlock(Pk2Entry[] entries) {
			this.block = entries;
		}

		public Pk2Entry getLastEntry() {
			return block[19];
		}

		public Iterator<Pk2Entry> getIterator() {

			return Arrays.asList(block).iterator();
		}
	}

	/*
	 * @Override public List<JMXFile> find(String regex, int limit) { return
	 * find(regex, FIRST_BLOCK_POS, limit); }
	 */

	/*
	 * private Pk2Entry findDirEntry(long pos, Stack<String> path) {
	 *
	 * EntryBlock block = readBlock(pos); Iterator ite = block.getIterator();
	 *
	 * while (ite.hasNext()) { Pk2Entry entry = (Pk2Entry) ite.next();
	 *
	 * if (entry.Type == 1) { if (!entry.Name.contains(".") &&
	 * !entry.Name.contains("..")) {
	 *
	 * if (path.peek().equalsIgnoreCase(entry.Name)) { path.pop();
	 *
	 * return (path.isEmpty()) ? entry : findDirEntry(entry.Position, path);
	 *
	 * }
	 *
	 * }
	 *
	 * } // System.out.println("Next Chain : " +entry.NextChain); } long NextChain =
	 * block.getLastEntry().NextChain; if (NextChain > 0) {
	 *
	 * Pk2Entry target = findDirEntry(NextChain, path); // getFile(NextChain,
	 * fileName); if (target != null) { return target; } }
	 *
	 * return null; }
	 *
	 * private List<JMXFile> findAt(String regex, long pos, int limit) {
	 *
	 * EntryBlock block = readBlock(pos); Iterator ite = block.getIterator();
	 * List<JMXFile> res = new ArrayList<>();
	 *
	 * while (ite.hasNext()) { Pk2Entry entry = (Pk2Entry) ite.next();
	 *
	 * if (entry.Type == 2) {
	 *
	 * if (Pattern.matches(regex, entry.Name)) { res.add(new JMXFile(entry));
	 * System.out.println("Matched : " + entry.Name); if (limit > 0 && res.size() ==
	 * limit) return res; } else {
	 * System.out.println("Entry Name Not Match With Regex : " + entry.Name + " " +
	 * regex); } }
	 *
	 * }
	 *
	 * long nextChain = block.getLastEntry().NextChain; if (nextChain > 0) {
	 *
	 * List<JMXFile> nextFiles = findAt(regex, nextChain, limit - res.size());
	 * res.addAll(nextFiles);
	 *
	 * }
	         * return res; }
         */

	/**
	 * Resolves a input pattern into a valid regex.
	 * If the input is a simple wildcard pattern (contains * but no valid regex chars), 
	 * it converts it to regex.
	 */
	String resolvePattern(String pattern) {
		if (pattern == null) return "";
		
		// Check for regex signatures: complex symbols, .* wildcard, or escaped dot
		boolean isRegex = pattern.matches(".*[\\[\\]\\(\\)\\{\\}\\|\\+\\?\\^\\$].*") 
						|| pattern.contains(".*") 
						|| pattern.contains("\\.");

		// Check if it's a simple wildcard pattern (contains * but not identified as regex)
		if (pattern.contains("*") && !isRegex) {
			// Convert simple wildcard pattern to regex
			String regex = pattern
				.replace("\\", "\\\\") // Escape backslashes
				.replace(".", "\\.")   // Escape dots
				.replace("*", ".*");   // Convert wildcard to regex match
			return regex;
		}
		return pattern;
	}

}
