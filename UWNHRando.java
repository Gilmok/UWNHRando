

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;



class DiffTool
{
	SNESRom f1;
	SNESRom f2;
	
	public DiffTool(String fname1, String fname2)
	{
		try
		{
			f1 = new SNESRom(fname1);
			f2 = new SNESRom(fname2);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public DiffTool(SNESRom r1, SNESRom r2)
	{
		f1 = r1;
		f2 = r2;
	}
	
	public void report(String diffOut)
	{
		byte[] b1 = f1.data;
		byte[] b2 = f2.data;
		String out = "";
		boolean rep = false;
		int dl = 0;
		int len = 0;
		int cutoff = 10;
		int same = cutoff;
		String s1 = "";
		String s2 = "";
		
		for(int i = 0; i < b1.length; i++)
		{
			if(b1[i] != b2[i])
			{
				same = 0;
				if(!rep)
				{
					rep = true;
					dl = i;
					len = 0;
				}
			}
			else
			{
				if(rep)
				{
					same++;
					if(same == cutoff)
					{
						out += len + " Byte diff at loc " + dl + " (file location " + (dl + 512) + "):----------\n";
						out += s1 + "\n";
						out += "------------------------------------------------\n";
						out += s2 + "\n";
						out += "\n";
						s1 = "";
						s2 = "";
						rep = false;
						dl = 0;
						len = 0;
					}
				}
			}
			if(same < cutoff)
			{
				len++;
				if(len % 40 == 0)
				{
					s1 += "\n";
					s2 += "\n";
				}
				s1 += byteStr(b1[i]) + " ";
				s2 += byteStr(b2[i]) + " ";
			}
		}
		
		if(diffOut == null)
			System.out.println(out);
		else
		{
			File f = new File(diffOut);
			try
			{
				if(!f.exists())
					f.createNewFile();
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(out);
				bw.close();
			}
			catch(Exception ex)
			{
				System.out.println(ex.getMessage());
			}
		}
	}
	
	private String byteStr(byte in)
	{
		String s = Integer.toHexString(in & 255);
		if(s.length() < 2)
			s = "0" + s;
		return s;
	}
	
	
	
	/*private String printWidth(String ss, int w)
	{
		int c = 0;
		for(int i = 0; i < ss.length(); i++)
		{
			if(ss.charAt(i).equals(' '))
		}
	}*/
}


class SNESRom
{
	public static final int DECOMPRESSION_LOC = 1738392;  //the game wants to decompress everything starting at DA8698
	public static final int MAP_LOC = 1163902 - 512; //compressed map data is at map loc D1C07E
	byte[] header;
	byte[] data;
	
	String filename;
	String homeDir;
	public byte romInvalid;
	
	String[][][] romOrigTheme;
	private boolean viewMiniMap;
	
	byte[] availGoodCount;
	
	int[] mapLocs;
	
	SNESRom(String fn) throws Exception
	{
		filename = fn;
		byte[] allData = loadData(fn);
		setHomeDir(fn);
		if(allData.length == (1024 * 1024 * 2))  //headerless
		{
			data = allData;
			return;
		}
		header = new byte[512];
		data = new byte[allData.length - 512];
		for(int i = 0; i < 512; i++)
			header[i] = allData[i];
		for(int i = 512; i < allData.length; i++)
			data[i - 512] = allData[i];
		mapLocs = new int[4];
		mapLocs[0] = MAP_LOC;
		noKWStorms = false;
		//setupInventoryList();
		//mapSwap = false;
		//availGoodCount = new byte[46];
	}
	
	public byte[] loadData(String fname) throws Exception
	{
		File file = new File(fname);
	    byte[] dest = new byte[(int) file.length()];
	    DataInputStream dis;
		//try 
		//{
			dis = new DataInputStream(new FileInputStream(file));
			dis.readFully(dest);
			dis.close();
		/*} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			System.out.println("Error: could not read nes file " + fname);
			e.printStackTrace();
		}*/
		return dest;
	}
	
	public void setHomeDir(String fname)
	{
		File file = new File(fname);
		homeDir = file.getAbsolutePath();
		String sep = File.separator;
		if(sep.equals("\\"))
			sep = "\\" + sep;
		String[] parts = homeDir.split(sep);
		homeDir = "";
		for(int i = 0; i <= parts.length - 2; i++)
			homeDir += parts[i] + File.separator;
		//homeDir += parts[parts.length - 2];
	}
	
	public void write3Bytes(int loc, int val)  //useful for putting compression deltas
	{
		for(int i = 0; i < 3; i++)
		{
			data[loc + i] = (byte) (val & 255);
			val >>= 8;
		}
	}
	
	public ArrayList<Integer> getEventRects()
	{
		int loc1 = 997632 - 512;
		//f3ac2 is event table 2 (Oct 1 - Apr 1)
		int loc2 = 998082 - 512;
		//f3c84 is event table 3
		int loc3 = 998532 - 512;
		
		ArrayList<Integer> rv = new ArrayList<Integer>();
		for(int i = 0; i < 450; i++)
		{
			int x1 = (i % 30) * 72;
			//int x2 = x1 + 71;
			int y1 = (i / 30) * 72;
			//int y2 = y1 + 71;
			//String locText = "y=" + y1 + " to " + y2 + ", x=" + x1 + " to " + x2;
			byte spring = (byte) (data[loc1 + i] & 64); 
			byte fall = (byte) (data[loc2  + i] & 64);
			byte omni = (byte) (data[loc3 + i] & -64);  //val & c0
			
			if(omni == 64) //40
			{
				//System.out.println("Storm in region " + locText);
				rv.add(1);
				rv.add(x1);
				rv.add(72);
				rv.add(y1);
				rv.add(72);
			}
			else if(omni == -128)  //80
			{
				//System.out.println("Wind stops in region " + locText);
				rv.add(2);
				rv.add(x1);
				rv.add(72);
				rv.add(y1);
				rv.add(72);
			}
			else if(omni == -64)
			{
				//System.out.println("Fog in region " + locText);
				rv.add(3);
				rv.add(x1);
				rv.add(72);
				rv.add(y1);
				rv.add(72);
			}
			/*if(spring > 0)
			{
				rv.add(4);
				rv.add(x1);
				rv.add(72);
				rv.add(y1);
				rv.add(72);
			}
			if(fall > 0)
			{
				rv.add(5);
				rv.add(x1);
				rv.add(72);
				rv.add(y1);
				rv.add(72);
			}*/
		}
		return rv;
	}
	
	public void stormReport()
	{
		System.out.println("=== Event report ===");
		//f3900 is event table 1 (Apr 1 - Oct 1) 
		int loc1 = 997632 - 512;
		//f3ac2 is event table 2 (Oct 1 - Apr 1)
		int loc2 = 998082 - 512;
		//f3c84 is event table 3
		int loc3 = 998532 - 512;
		
		for(int i = 0; i < 450; i++)
		{
			int x1 = (i % 30) * 72;
			int x2 = x1 + 71;
			int y1 = (i / 30) * 72;
			int y2 = y1 + 71;
			String locText = "y=" + y1 + " to " + y2 + ", x=" + x1 + " to " + x2;
			byte spring = data[loc1 + i]; 
			byte fall = data[loc2  + i];
			byte omni = (byte) (data[loc3 + i] & -64);  //val & c0
			
			if(omni == 64) //40
			{
				System.out.println("Storm in region " + locText);
			}
			else if(omni == -128)  //80
			{
				System.out.println("Wind stops in region " + locText);
			}
			else if(omni == -64)
			{
				System.out.println("Fog in region " + locText);
			}
		}
	}
	
	public void writeShort(int loc, int val)
	{
		data[loc] = (byte) (val & 255);
		val >>= 8;
		data[loc + 1] = (byte) (val & 255);
	}
	
	public void writeBytes(int loc, byte[] bts)
	{
		for(int i = 0; i < bts.length; i++)
			data[loc + i] = bts[i];
	}
	
	public void writeBytes(int loc, ArrayList<Byte> bts)
	{
		for(int i = 0; i < bts.size(); i++)
			data[loc + i] = bts.get(i);
	}
	
	public short readShort(int loc)
	{
		short rv = (short) (data[loc] & 255);
		rv |= ((data[loc + 1] & 255) << 8);
		return rv;
	}
	
	public void dumpRom(String fname, boolean useHomeDir)
	{
		if(useHomeDir)
		{
			if(fname.indexOf(File.separator) >= 0)
            	fname = fname.substring(fname.lastIndexOf(File.separator) + 1);
			fname = homeDir + fname;
		}
		FileOutputStream fos;
		try 
		{
			fos = new FileOutputStream(fname);
			if(header != null)
				fos.write(header);
			fos.write(data);
			fos.close();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	/*public void saveRom(String fname)
	{
		File f = new File(fname);
		String fn = f.getName();
		if(fn.startsWith("UWNHRando."))
		{
			String[] parts = fn.split("\\.");
			parts[2] = finalFlags;
			fn = "";
			for(int i = 0; i < parts.length - 1; i++)
				fn += parts[i] + ".";
			fn += "nes";
			
			String path = f.getParent() + File.separator + fn;
			outFileName = path;
		}
		dumpRom(outFileName);
		NESRom rv = new NESRom(outFileName);
		rv.gameMap = gameMap;
		return rv;
	}*/
	
	public String hexAddr(String hexString, boolean twoBytes)  //this converts a hex string to a hex address by splitting and reversing it
	{
		String rv = "";
		if(hexString.length() == 1)
			hexString = "0" + hexString;
		for(int i = hexString.length() - 2; i >= 0; i -= 2)  //grab the last two chars of hex value
		{
			rv += hexString.charAt(i);
			rv += hexString.charAt(i + 1);
			rv += " ";  //insert space
			if(twoBytes && rv.length() == 6)
				break;
		}
		rv = rv.trim();
		int min = 7;
		if(twoBytes)
			min = 5;
		while(rv.length() < min)
			rv = rv + " 00";
		return rv.trim();
	}
	
	static final int ROM_BASE = 12582912;  //0xc00000
	//data overflow at 1eca00-1f01ff (14,335 bytes free for data overflow)
	static final int DATA_OVERFLOW = 2017792 - 512;
	static final int DATA_MAX = 14435;
	int dataUsed = 0;
	//code overflow at 1fe800-1fffff (6656 bytes free for code overflow)
	static final int CODE_OVERFLOW = 2091008 - 512;
	static final int CODE_MAX = 6656;
	int codeUsed = 0;
	public boolean noKWStorms;
	
	public void allocateData(int amt)
	{
		dataUsed += amt;
		if(dataUsed > DATA_MAX)
			romInvalid = 10;
	}
	
	public void allocateCode(int amt)
	{
		codeUsed += amt;
		if(codeUsed > CODE_MAX)
			romInvalid = 11;
	}
	
	public void editTitleScreen()
	{
		int loc1 = CODE_OVERFLOW;   //1fe800 or dfe800
		String hex = Integer.toHexString(loc1 + ROM_BASE);
		String randLoc = hexAddr(hex, true);
		String rando = "Randomizer";
		byte[] randB = textToBytes(rando);
		writeBytes(loc1, randB);
		
		int used = randB.length + 2;

		int data1 = loc1 + randB.length + 1;
		data[data1] = 0;
		
		data1++;
		String dd1 = "00 04 01 00 00 60 7f 00";
		byte[] db1 = strToBytes(dd1);
		hex = Integer.toHexString(data1 + ROM_BASE + db1.length - 1);
		String data1Loc = hexAddr(hex, true);
		writeBytes(data1, db1);
		
		used += db1.length;
		
		int data2 = data1 + db1.length;
		String dd2 = "0c 00 01 00 00 4e 7f 00";
		byte[] db2 = strToBytes(dd2);
		hex = Integer.toHexString(data2 + ROM_BASE + db2.length - 1);
		String data2Loc = hexAddr(hex, true);
		writeBytes(data2, db2);
		
		used += db2.length;
		
		int data3 = data2 + db2.length;
		String dd3 = "00 ff 68 00 f0 00 68 00 28 00 40 02 1d 03 11 00 " +
				 "0d 00 0d 00 05 00 03 00 08 00 00 c0";
		byte[] db3 = strToBytes(dd3);
		hex = Integer.toHexString(data3 + ROM_BASE);
		String data3Loc = hexAddr(hex, false);
		writeBytes(data3, db3);
		
		used += db3.length;
		
		//insert function to handle VRAM writes
		int fx1 = data3 + db3.length;  //we may use this function elsewhere
		hex = Integer.toHexString(fx1 + ROM_BASE);
		String fx1Loc = hexAddr(hex, false);
		int bank = (((data1 + ROM_BASE) & 16711680) >> 16);  //rom base & 0xFF0000 >> 16
		String dbank = Integer.toHexString(bank);
		String sfx1 = "a3 04 8f 33 0b 00 a3 06 aa 8b 3b a8 " + 
				"a9 07 00 44 00 " + dbank + " bb 9a " + 
				"a9 00 00 48 48 f4 03 00 f4 06 00 22 00 80 c0 " +
				"3b 18 69 10 00 1b ab 6b";
		byte[] bts1 = strToBytes(sfx1);
		writeBytes(fx1, bts1);
		
		used += bts1.length;
		
		//insert function to write on title screen
		int fx2 = fx1 + bts1.length;
		hex = Integer.toHexString(fx2 + ROM_BASE);
		String fx2Loc = hexAddr(hex, false);
		String sfx2 = "f4 " + data1Loc + " f4 00 72 22 " + fx1Loc + " 68 68 8b a9 ff 00 " +
				"a2 00 60 a0 00 4d 54 7f 7f ab " + 
				"a2 1a 00 bf " + data3Loc + " 9f de ea 7e ca ca 10 f4 " +
				"a9 02 00 8f 31 a2 7e " +
				"3a 8f 23 a2 7e 48 f4 00 00 f4 e6 08 f4 " + dbank + " 56 f4 " +
				randLoc + " 22 17 3d c2 18 3b 69 0a 00 1b " +
				"a9 4b 26 a2 16 00 " +
				"9f 00 4e 7f 3a ca ca 10 f7 f4 " + data2Loc + " f4 4c 7e 22 " + fx1Loc +
				" 68 68 a9 00 7c 8f 33 0b 00 f4 00 22 " +
				"f4 10 00 22 fe 8c cd 68 68 6b";
		byte[] bts2 = strToBytes(sfx2);
		writeBytes(fx2, bts2);
		
		used += bts2.length;
		allocateCode(used);
		
		//int last = fx2 + bts2.length;
		//int uu = used + CODE_OVERFLOW;
		
		//call the function when "new game" is placed
		int callLoc = 897022 - 512;  //dadfe
		String fcall = "22 " + fx2Loc + " ea ea ea ea ea ea ea ea";
		byte[] bfc = strToBytes(fcall);
		writeBytes(callLoc, bfc);
		
		//edit the palette so you can see it
		String pal = "7b 6f";
		byte[] pb = strToBytes(pal);
		int l1 = 711673 - 512;
		writeBytes(l1, pb);
		l1 = 711705 - 512;
		writeBytes(l1, pb);
	}
	
	public void editTitleCards()
	{
		String a = "Welcome to the Uncharted Waters New Horizons Randomizer! I hope you enjoy the many new ways "
				 + "this randomizer allows you to play.";
		String b = "With a Civilization style world creator, enjoy exploring new worlds of your creation! "
				+ "The minimap changes too so you don't get lost.";
		String c = "This randomizer also features a world Theme creator. Rename any port, kingdom, discovery, "
				+ "and even rename the commodities you trade!";
		String d = "Future plans for this randomizer include renaming of "
				+ "treasure items, editing discovery text, crew data, and even editing the game scripts!";
		String e = "This randomizer is in early alpha. It has known issues and has not been extensivly tested. "
				+ "See the FAQ for details, and as always, watch out for storms!";
		String f = "Thanks again for playing this randomizer. Also try Ultima 4 NES Randomizer and DW Monopoly, "
				+ "both available on the Gilmok github. Sincerely yours, Gilmok.";
		String[] set = {a, b, c, d, e, f};
		
		int loc = 965721 - 512;
		int ptrLoc = 941139 - 512;
		for(int i = 0; i < set.length; i++)
		{
			write3Bytes(ptrLoc, loc + ROM_BASE);
			ptrLoc += 3;
			byte[] bb = textToBytes(set[i]);
			writeBytes(loc, bb);
			loc += bb.length;
			//loc++;
			data[loc] = 0;
			loc++;
		}
		
	}
	
	public GameMap randomizeMap(int nContinents, int nIslands, int iceCapThick, boolean randomizePorts)
	{
		romInvalid = 0;
		//randomize world map
		int sz = 0;
		GameMap map; //= new GameMap();
		Compressor cc; //= new Compressor(this);
		ArrayList<ArrayList<Byte>> bts;
		do
		{
			map = new GameMap();
			cc = new Compressor(this);
			map.makeMap(nContinents, nIslands, iceCapThick);
			System.out.println("Generating map with " + nContinents + " continents, " + nIslands + " islands, and polarThick of " + iceCapThick);
			bts = cc.compressMap(map);  //this creates the compressed map blocks
			sz = cc.getMapSize(bts);
			System.out.println("Map size is " + sz + " bytes");
			
		} while(sz > 91600);  //77300+14300 byte overflow
		
		int mapBaseLoc = 1163902 - 512;
		int[] availSpace = {77313, DATA_MAX - dataUsed};
		//int[] availLocs = {0, DATA_OVERFLOW + dataUsed - mapBaseLoc};
		cc.collateMap(bts, availSpace, map);
		int[][] deltas = cc.getDeltas();
		//int[] overflows = cc.overflowDeltas;
		//int[] overflowLocs = {0, DATA_OVERFLOW + dataUsed - mapBaseLoc};
		int[][] deltaI = cc.getDeltaIndeces();
		map.blocks = cc.collectBlocks(bts, deltaI);
		map.deltas = deltas;
		
		
		//int mapBaseLoc = 1163902 - 512; //11c72e; the 512 header is shaved off during decompression
		//rom.outputData(mapBaseLoc, 50);
		
		//do not do this yet
		/*for(int i = 0; i < cm[0].length; i++)
			data[mapBaseLoc + i] = cm[0][i];
		
		for(int i = 0; i < cm[1].length; i++)
			data[DATA_OVERFLOW + dataUsed + i] = cm[1][i];
		this.allocateData(cm[1].length);*/
		
		/*int deltaLoc = 249786 - 512;  //3cfba
		
		for(int i = 0; i < deltas.length; i++)  //no change to this as deltas have already been changed
		{
			for(int j = 0; j < deltas[0].length; j++)
			{
				write3Bytes(deltaLoc, deltas[i][j]);
				deltaLoc += 3;
			}
		}*/
		
		//if(randomizePorts)
		//{
		//right now the port order is capital + cities, capital + cities, etc. we need to
		//redorder them to the original order
		map.reorderPortData();  
		GameMap.PortData[] pds = new GameMap.PortData[map.allPortData.size()];
		//int[] allCultures = new int[pds.length];
		for(int i = 0; i < pds.length; i++)
		{
			pds[i] = map.allPortData.get(i);
			//allCultures[i] = pds[i].possCultures;
		}
		Port[] ports  = null;
		if(randomizePorts)
			ports = randomizePorts(pds);
		else
			ports = UWNHRando.collectPorts(this, false);
		ArrayList<Integer> caps = new ArrayList<Integer>();
		
		int dest = 577875 - 512;   //port placement data
		int dest2 = 580743 - 512;  //port support data
		int destp = 735143 - 512;  //port palette data
		
		IndexedList il = new IndexedList();
		for(int i = 0; i < pds.length; i++)
		{
			//all ports of type 2 get some Spanish support
			if(i < ports.length)
			{
				if(pds[i].finalCulture == 2)
					pds[i].startingSupport[1] = (int) (UWNHRando.rand() * 30 + 10);
				else
					pds[i].finalCulture = ports[i].tileType;
				//we need this palette stuff as well as culture stuff for non-map randomization
				System.out.println("# " + i + ":" + ports[i].name + " is at " + pds[i].x + "," + pds[i].y + "; typ=" + pds[i].finalCulture);
				int pal = data[destp + i];
				pal = pds[i].palette = pds[i].selectPalette(pal);
				data[destp + i] = (byte) pal;
			}
			if(!pds[i].isDiscovery)
			{
				writeShort(dest, pds[i].x);
				dest += 2;
				writeShort(dest, pds[i].y);
				dest += 19;
				data[dest] = pds[i].iaData;
				dest++;
				if(!pds[i].isSupply)
				{
					for(int j = 0; j < 6; j++)
						data[dest2 + j] = (byte) pds[i].startingSupport[j];
					dest2 += 37;
				}
				pds[i].setOrigIndex(i);
				int xx = il.add(pds[i]);
				if(xx == -1)
					System.out.println("Failed to add port #" + i + " x=" + pds[i].x + " y=" + pds[i].y);
			}
		}
		System.out.println("Now inserting port discovery table");
		int dest3 = 697310 - 512;  //additional port x,y data
		for(int i = 0; i < il.size(); i++)
		{
			GameMap.PortData pd = (GameMap.PortData) il.get(i);
			writeShort(dest3, pd.x);
			dest3 += 2;
			writeShort(dest3, pd.y);
			dest3 += 2;
			writeShort(dest3, pd.origIndex);
			dest3 += 2;
		}
		System.out.println("Now inserting starting positions");
		insertNewStartPositions(pds);
		System.out.println("Inserted new start positions");
		
		//port and discovery findability data
		ArrayList<Byte> findList[][] = new ArrayList[17][34];
		for(int yy = 0; yy < 17; yy++)
			for(int xx = 0; xx < 34; xx++)
				findList[yy][xx] = new ArrayList<Byte>();
		
		for(int i = 0; i < pds.length; i++)
		{
			int startx = pds[i].x >> 6; 
			int remx = pds[i].x & 63;
		    int starty = pds[i].y >> 6;
			int remy = pds[i].y & 63;
			int	endx = startx;
			int endy = starty;
			if(remx < 20 && startx > 0) 
				startx--;
			if(remx > 44 && endx < 33) 
				endx++; 
			if(remy < 20 && starty > 0) 
				starty--;
			if(remy > 44 && endy < 16)
				endy++;
			for(int yy = starty; yy <= endy; yy++)
				for(int xx = startx; xx <= endx; xx++)
					findList[yy][xx].add((byte) i);
		}
		
		short[][] useList = new short[17][34];
		
		//find duplicates in the findLists
		ArrayList<ArrayList<Byte>> allLists = new ArrayList<ArrayList<Byte>>();
		for(int yy = 0; yy < 17; yy++)
		{
			for(int xx = 0; xx < 34; xx++)
			{
				ArrayList<Byte> ll = findList[yy][xx];
				int foundList = -1;
				for(int i = 0; i < allLists.size(); i++)
				{
					ArrayList<Byte> mm = allLists.get(i);
					if(ll.size() != mm.size())
						continue;
					if(mm.size() == 0)
						foundList = i;
					for(int j = 0; j < mm.size(); j++)
					{
						if(ll.get(j) != mm.get(j))
							break;
						if(j == mm.size() - 1)
							foundList = i;
					}
					if(foundList != -1)
						break;
				}
				if(foundList == -1)
				{
					useList[yy][xx] = (short) allLists.size();
					allLists.add(ll);
				}
				else
					useList[yy][xx] = (short) foundList;
			}
		}
		
		int listSize = 0;
		boolean useSmall = false;
		int[] sizes = new int[allLists.size()];
		for(int i = 0; i < allLists.size(); i++)
		{
			sizes[i] = listSize;
			listSize += allLists.get(i).size() + 1;
		}
		//we need to use small if the lists overflow; it's OK to do this as the list otherwise
		//just contain a bunch of 0s (no Port's index will ever be > 255)
		if(listSize * 2 > 1852)
			useSmall = true;
		System.out.println("Dumping all lists; using small=" + useSmall);
		//output list of lists -> c2f12f (2f32f)
		int find1 = 193327 - 512;
		for(int i = 0; i < allLists.size(); i++)
		{
			ArrayList<Byte> ll = allLists.get(i);
			for(int j = 0; j < ll.size(); j++)
			{
				if(useSmall)
				{
					data[find1] = ll.get(j);
					find1++;
				}
				else
				{
					int vv = ll.get(j);
					if(vv < 0)
						vv &= 255;
					writeShort(find1, vv);
					find1 += 2;
				}
			}
			data[find1] = -1;
			find1++;
			if(!useSmall)
			{
				data[find1] = -1;
				find1++;
			}
		}
		
		if(useSmall)  //nop out c1fce6 and c1fce8, which doubles the address deltas 
		{
			//nop out the doublers c1fce6 and c1fce8
			for(int i = 0; i < 4; i++)
				data[130790 - 512 + i] = (byte) 234;  //ea (nop)
			//c962e7 adc 2 -> adc 1 to advance to next list entry
			data[615656 - 512] = 1;
		}
		
		//output the list of deltas -> c2f86b (2fa6b)
		int find2 = 195179 - 512;
		for(int yy = 0; yy < 17; yy++)
		{
			for(int xx = 0; xx < 34; xx++)
			{
				int idx = useList[yy][xx] & 65535;
				writeShort(find2, sizes[idx]);
				find2 += 2;
			}
		}
		System.out.println("Dumping port discoverability data");
		//Port View Data - cf3500
		int view1 = 997120 - 512;
		int view2 = view1 + 256;
		for(int i = 0; i < 255; i++)
		{
			data[view1 + i] = -1;
			data[view2 + i] = -1;
		}
		int firstDisc = -1;
		for(int i = 0; i < pds.length; i++)
		{
			
			int vv = pds[i].x + pds[i].y;
			int oo = (vv % 255); 
			int o2 = oo % 255;
			if(!pds[i].isDiscovery)
			{
				//int skips = 0;
				while(data[view1 + o2] != -1)
				{
					oo += 19;
					o2 = oo % 255;
					//skips++;
				}
				data[view1 + o2] = (byte) i;
				//System.out.println("Port " + i + " found free spot at " + o2 + " after " + skips + " skips");
			}
			else
			{
				if(firstDisc == -1)
					firstDisc = i;
				while(data[view2 + o2] != -1)
				{
					oo += 19;
					o2 = oo % 255;
				}
				data[view2 + o2] = (byte) (i - firstDisc);
				//System.out.println("Village " + i + " found at free spot at " + o2);
				//break;
			}
		}
		/*ArrayList<Byte> fv = new ArrayList<Byte>();
		for(int i = 0; i < 100; i++)
			fv.add((byte) i);
		for(int i = 0; i < 255; i++)
		{
			if(data[view2 + i] != -1)
			{
				int rem = data[view2 + i];
				fv.set(rem, (byte) -1);
			}
		}  
		for(int i = 0; i < fv.size(); i++)
		{
			if(fv.get(i) == -1)
			{
				fv.remove(i);
				i--;
			}
		}
		if(fv.size() > 0)
			System.out.println("Villages missing entries:" + fv.toString());*/
		
		System.out.println("Now inserting villages");
		//edit discoveries
		int vill = 584459 - 512;
		for(int i = firstDisc; i < pds.length; i++)
		{
			writeShort(vill, pds[i].x);
			vill += 2;
			writeShort(vill, pds[i].y);
			vill += 5;
		}
		
		System.out.println("Now output minimap");
		GameMiniMap gmp = new GameMiniMap(this);
		gmp.createMiniMap(map, viewMiniMap);
		/*if(viewMiniMap)
		{
			GameMapWindow gmw = new GameMapWindow(map);
		}*/
		gmp.testCompressedBytes(this, map);
		map.editKnownWorld(this);
		System.out.println("And finally dump map");
		map.dumpMap(this, deltaI);
		
		return map;
		//??? - cf3600  don't worry about this for now
		
	}
	
	private void insertNewStartPositions(GameMap.PortData[] pds) 
	{
		int[] nli = {0,1,29,33,8,2};
		int[] locs = {565170,566500,569160,571820,570490,567830};
		for(int i = 0; i < nli.length; i++)
		{
			GameMap.PortData pd = pds[nli[i]];
			int open = pd.openDirs;
			int test = 3;
			int dx = 0;
			int dy = 0;
			int[] xa = {-2,0,2,0};
			int[] ya = {0,2,0,-2};
			int n = 0;
			while(n < 4)  //test double openings
			{
				if((open & test) == test)
				{
					dx = xa[n];
					dy = ya[n];
					break;
				}
				else
				{
					test <<= 2;
					n++;
				}
			}
			if(n == 4) //unable to find double, try singles
			{
				int[] xs = {-2,-2,-1,1,2,2,1,-1};
				int[] ys = {-1,1,2,2,1,-1,-2,-2};
				n = 0;
				test = 1;
				while(n < 8)
				{
					if((open & test) == test)
					{
						dx = xs[n];
						dy = ys[n];
						break;
					}
					else
					{
						test <<= 1;
						n++;
					}
				}
			}
			writeShort(locs[i] - 512, pd.x + dx);
			writeShort(locs[i] - 510, pd.y + dy);
		}
	}

	private Port[] randomizePorts(GameMap.PortData[] pdata)
	{
		Port[] ps = null;
		
		while(ps == null)
			ps = UWNHRando.selectPortStyles(pdata, this);
		//int[] rv = new int[ps.length];
		//then output the port tilesets, the port deltas and port data
		//tilesets -> 735038 - 512
		for(int i = 0; i < ps.length; i++)
		{
			data[735038 - 512 + i] = (byte) pdata[i].finalCulture;
			//rv[i] = ps[1].tileType;
		}
		//port deltas
		int totalDelta = 0;
		int deltaLoc = 650959 - 512;
		int portDataLoc = 1738904 - 512;
		Compressor comp = new Compressor(this);
		for(int i = 0; i < ps.length; i++)
		{
			
			boolean converted = ps[i].convertTileType(pdata[i].finalCulture, null);
			if(converted)
			{
				ArrayList<Byte> p2 = ps[i].getConvertedPort();
				byte[] pa = new byte[p2.size()];
				for(int j = 0; j < p2.size(); j++)
					pa[j] = p2.get(j);
				ArrayList<Byte> newPort = comp.compressBytes(pa);
				ps[i].compressedBytes = newPort;
				ps[i].tileType = (byte) pdata[i].finalCulture;
			}
			//and the data
			for(int j = 0; j < ps[i].compressedBytes.size(); j++)
				data[portDataLoc + totalDelta + j] = ps[i].compressedBytes.get(j);
			//store off the delta
			write3Bytes(deltaLoc, totalDelta);
			totalDelta += ps[i].compressedBytes.size();
			deltaLoc += 3;
			
		}
		return ps;
	}
	
	public void randomizeStartingShips()
	{
		int[] ssLocs = {1038974,1048010,1054600,2032304,2034844};
		for(int i = 0; i < ssLocs.length; i++)
			data[ssLocs[i] - 512] = (byte) (UWNHRando.rand() * 19);
	}
	
	public byte[] strToBytes(String in)
	{
		String[] strs = in.split(" ");
		byte[] rtnVal = new byte[strs.length];
		for(int i = 0; i < strs.length; i++)
		{
			short ss = Short.parseShort(strs[i], 16);
			if(ss <= 127)
				rtnVal[i] = (byte) ss;
			else
				rtnVal[i] = (byte) (ss - 256);
		}
		return rtnVal;
	}
	
	public byte[] textToBytes(String in)
	{
		char[] cc = in.toCharArray();
		byte[] rv = new byte[cc.length];
		for(int i = 0; i < cc.length; i++)
			rv[i] = (byte) (cc[i] & 255);
		return rv;
	}
	
	public void listLandTilesets()
	{
		int gtiles = 983552 - 512;
		byte[][] tiles = new byte[256][4];
		int[] indeces = new int[256];
		for(int i = 0; i < 1024; i += 4)
		{
			indeces[i >> 2] = i >> 2;
			for(int j = 0; j < 4; j++)
				tiles[i >> 2][j] = data[gtiles + i + j];
		}
		
		//insertion sort
		int i = 1;
		while(i < indeces.length)
		{
		    int x = tiles[i][0] & 255;
		    int y = indeces[i];
		    int j = i - 1;
		    while(j >= 0 && (tiles[indeces[j]][0] & 255) > x)
		    {
		        indeces[j + 1] = indeces[j];
		        j--;
		    }
		    indeces[j + 1] = y;
		    i++;
		}
		
		for(int k = 0; k < 256; k++)
		{
			System.out.println(k + ":" + Arrays.toString(tiles[indeces[k]]));
		}	
	}
	
	public String bytesToStr(byte[] in)
	{
		String rv = "";
		for(int i = 0; i < in.length; i++)
		{
			String s = Integer.toHexString(in[i]);
			if(s.length() == 1)
				s = "0" + s;
			else if(s.length() > 2)
				s = s.substring(s.length() - 2, s.length());
			rv += s + " ";
		}
		return rv;
	}
	
	public String byteToStr(byte in)
	{
		String s = Integer.toHexString(in);
		if(s.length() == 1)
			s = "0" + s;
		else if(s.length() > 2)
			s = s.substring(s.length() - 2, s.length());
		return s;
	}
	
	public byte[] getBytes(int start, int end, boolean shaveHeader)
	{
		if(shaveHeader)
		{
			start -= header.length;
			end -= header.length;
		}
		byte[] rv = new byte[end - start + 1];
		for(int i = start; i <= end; i++)
			rv[i - start] = data[i];
		return rv;
	}
	
	public int findBytes(String bts, int startAt)
	{
		byte[] findMe = strToBytes(bts);
		int m = findMe.length - 1;
		for(int i = startAt; i < data.length; i++)
		{
			int si = i;
			for(int j = 0; j < findMe.length; j++)
			{
				if(data[i] != findMe[j])
					break;
				i++;
				if(j == m)
					return si;
			}
		}
		return -1;
	}
	
	public String reportFound(String bts)
	{
		int s = 0;
		String rv = "";
		while(true)
		{
			s = findBytes(bts, s);
			if(s == -1)
				break;
			else
				rv += "Data found at " + s + " \n";
			s++;
		}
		return rv;
	}
	
	public byte[] getDecompressedText(byte[] compressed)
	{
		int a = 0;
		byte c = 0;
		int pos = 0;
		byte bread = 0;
		byte ctrl = 0;
		ArrayList<Byte> out = new ArrayList<Byte>();
		while(pos < compressed.length)
		{
			c = 0;
			for(int i = 0; i < 5; i++)
			{
				a = ((compressed[pos] << bread) & -128);
				a >>= 7;
				a &= 1;
				//if(a > 1 || a < 0)
					//System.out.println(a);
				c <<= 1;
				c |= a;
				bread++;
				if(bread == 8)
				{
					bread = 0;
					pos++;
					if(pos == compressed.length)
					{
						break;
					}
				}
			}
			//if(c < 0)
				//System.out.println(c);
			if(c >= 28)
				ctrl = (byte) (c - 28);
			else if(c == 0)
				out.add((byte) 0);
			else if(c == 27)
				out.add((byte) 32);
			else
			{
				switch(ctrl)
				{
				case 0:  out.add((byte) ('A' + c - 1));  break;
				case 1:  out.add((byte) ('a' + c - 1));  break;
				case 2:
					if(c <= 10)
						out.add((byte) ('0' + c - 1));
					else
					{
						String s1 = ",.;\':!?-+%/|\"()";
						out.add((byte) s1.charAt(c - 11));
					}
					break;
				case 3:
					String s2 = "[]{}*<>#=~@$\\";
					if(c > 0 && c <= s2.length())
						out.add((byte) s2.charAt(c - 1));
					else
					{
						byte[] vv = {-122, -30, 10, 27};
						
						out.add(vv[c - 22]);
					}
				}
			}
		}
		byte[] rv = new byte[out.size()];
		for(int i = 0; i < out.size(); i++)
			rv[i] = out.get(i);
		return rv;
	}
	
	public void outputData(int startLoc, int nVals)  //normally used for testing
	{
		String rv = "";
		for(int i = 0; i < nVals; i++)
		{
			byte b = data[startLoc + i];
			rv += Integer.toHexString((int) (b & 255)) + " ";
		}
		System.out.println("Data starting at " + startLoc + "(" + nVals + " values)");
		System.out.println(rv);
	}
	
	public byte[] getCompressedText(int startLoc, int endLoc, boolean shaveHeader)
	{
		if(shaveHeader)
		{
			startLoc -= header.length;
			endLoc -= header.length;
		}
		//5-bit compression
		ArrayList<Byte> out = new ArrayList<Byte>();
		long cb = 0;
		byte pos = 0;
		byte ctrl = 0;
		for(int i = startLoc; i <= endLoc; i++)
		{
			char c = (char) data[i];
			byte val = 0;
			byte nc = ctrl;
			if(c == 0)  //null
				val = 0;
			else if(c == 32)  //space
				val = 27;
			else if(Character.isUpperCase(c))
			{
				nc = 0;
				val = (byte) (c - 'A' + 1);
			}
			else if(Character.isLowerCase(c))
			{
				nc = 1;
				val = (byte) (c - 'a' + 1); 
			}
			else if(Character.isDigit(c))
			{
				nc = 2;
				val = (byte) (c - '0' + 1);
			}
			else
			{
				String s1 = ",.;\':!?-+%/|\"()";
				String s2 = "[]{}*<>#=~@$\\";
				int a = s1.indexOf(c);
				if(a != -1)
				{
					nc = 2;
					val = (byte) (a + 11);
				}
				else
				{
					int b = s2.indexOf(c);
					if(b != -1)
					{
						nc = 3;
						val = (byte) (b + 1);
					}
					else if(Integer.valueOf(c) == 27)
					{
						nc = 3;
						val = 25;
					}
					else if(Integer.valueOf(c) == 10)
					{
						nc = 3;
						val = 24;
					}
					else if(Integer.valueOf(c) == 65414) //0x86
					{
						nc = 3;
						val = 22;
					}
					else if(Integer.valueOf(c) == 65506) //0xe2
					{
						nc = 3;
						val = 23;
					}
					else
					{
						System.out.println("Error: " + c + " (" + Integer.valueOf(c) + ") at loc " + (i + 512) + " was not found in any charset");
						continue;
					}
				}
			}
			//write the ctrl bit if changed
			if(nc != ctrl)
			{
				ctrl = nc;
				cb <<= 5;
				cb |= ((ctrl + 28) & 31);
				pos++;
				if(pos == 8)
				{
					for(int j = 4; j >= 0; j--)
					{
						long kk = (cb >> (j * 8)) & 255;
						out.add(new Byte((byte) kk));
					}
					pos = 0;
					cb = 0;
				}
			}
			cb <<= 5;
			cb |= (val & 31);
			pos++;
			if(pos == 8)
			{
				for(int j = 4; j >= 0; j--)
				{
					long kk = (cb >> (j * 8)) & 255;
					out.add(new Byte((byte) kk));
				}
				pos = 0;
				cb = 0;
			}
		}
		if(pos > 0)
		{
			int fb = (5 * pos);
			while(fb >= 8)
			{
				fb -= 8;
				long kk = (cb >> fb) & 255;
				out.add(new Byte((byte) kk));
			}
			if(fb > 0)
			{
				long kk = (cb << (8 - fb)) & 255;
				out.add(new Byte((byte) kk));
			}
		}
		byte[] rv = new byte[out.size()];
		for(int i = 0; i < out.size(); i++)
			rv[i] = out.get(i);
		return rv;
	}

	public SNESRom randomize(String flags, String outFile, String failure[], boolean toRomDir, String[][][] theme) 
	{
		String newDir = outFile.substring(0, outFile.lastIndexOf(File.separator));
		String fn = outFile.substring(newDir.length() + 1);
		UWNHRando.setOutputRomName(fn);
		if(toRomDir)  //new ROM going to the ROM's original directory
		{
			if(newDir != homeDir)
			{
				outFile = outFile.substring(newDir.length() + 1);
				outFile = homeDir + outFile;
			}
		}
		else   //new ROM going to Randomizer's directory
		{
			homeDir = newDir;
		}
		PrintStream console = System.out;
		PrintStream log = null;
		try 
		{
			log = new PrintStream(new File(homeDir + "makeLog.txt"));
			System.setOut(log);
			System.out.println("== Making rom " + fn + " ==");
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		editTitleScreen();
		editTitleCards();
		String[][][] origTheme = getOrigThemeData();
		//applyTheme(theme);
		LinkedHashMap<String, Integer> flagMap = new LinkedHashMap<String, Integer>();
		String[] okFlags =  {"M", "C", "I", "O", "S", "V", "R", "K", "s", "b", "t"};
		boolean[] usesVal = {false, true, true, true, false, false, false, false, false, false, false};
		for(int i = 0; i < okFlags.length; i++)
			flagMap.put(okFlags[i], -1);
		while(flags.length() > 0)
		{
			char ch1 = flags.charAt(0);
			char ch2 = 'A';
			String ss = "";
			if(flags.length() > 1)
				ch2 = flags.charAt(1);
			int val = 1;
			int grabChars = 1;
			while(Character.isDigit(ch2))
			{
				ss += ch2;
				val = Integer.parseInt(String.valueOf(ss));
				grabChars++;
				if(flags.length() > grabChars)
					ch2 = flags.charAt(grabChars);
				else
					break;
			}
			for(int i = 0; i < okFlags.length; i++)
			{
				if(okFlags[i].equals("" + ch1))
				{
					flagMap.put("" + ch1, val);
					if(!usesVal[i])
						flagMap.put("" + ch1, -2);
					break;
				}
			}
			flags = flags.substring(grabChars);
		}
		String[] ffs = new String[flagMap.keySet().size()];
		ffs = flagMap.keySet().toArray(ffs);
		String finalFlags = "";
		//int mapPad = 0;
		boolean randomizeMap = false;
		viewMiniMap = false;
		int[] mapParams = new int[3];
		for(int i = 0; i < ffs.length; i++)
		{
			char ch1 = ffs[i].charAt(0);
			int val = flagMap.get(ffs[i]);
			if(val == -1)
				continue;
			else if(val >= 0)
				finalFlags += "" + ch1 + val;
			else
				finalFlags += "" + ch1;
			switch(ch1)
			{
			case 'M':
				randomizeMap = true;
				break;
			case 'C':
				mapParams[0] = val;
				break;
			case 'I':
				mapParams[1] = val;
				break;
			case 'O':
				mapParams[2] = val;
				break;
			case 'S':
				randomizeStartingShips();
				break;
			case 'V':
				viewMiniMap = true;
				break;
			case 'R':
				randomizeMarketRates();
				break;
			case 'K':
				randomizeMarketTypes();
				break;
			case 's':
				this.noKWStorms = true;
				break;
			case 'b':
				elimShipBuildCooldowns();
				break;
			case 't':
				elimShopClosures();
				break;
			}
		}
		if(randomizeMap)
		{
			applyTheme(theme, origTheme);
			randomizeMap(mapParams[0], mapParams[1], mapParams[2], true);
		}
		else
		{
			GameMap.PortData[] pd = getOrigPortData();
			applyTheme(theme, origTheme);
			randomizePorts(pd);
		}
		testPortMarkets();
		if(romInvalid > 0)
		{
			switch(romInvalid)
			{
			case 1:  failure[0] = "Compressed map was too large";  break;
			case 2:  failure[0] = "Too many minimap tiles generated"; break;
			case 3:  failure[0] = "Couldn't fit compressed minimap tiles";  break;
			case 4:  failure[0] = "Couldn't fit minimap city entries"; break;
			case 10: failure[0] = "Data overflow was exhausted"; break;
			case 11: failure[0] = "Code overflow was exhausted"; break;
			}
			return null;
		}
		dumpRom(outFile, true);
		try
		{
			SNESRom rv = new SNESRom(outFile);
			log.flush();
			log.close();
			System.setOut(console);
			
			//rv.gameMap = gameMap;
			return rv;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.setOut(console);
			return null;
		}
		//return this;
	}
	
	public int addrToLoc(String addr)  //converts a game address to a file location
	{
		int loc = Integer.parseInt(addr, 16);
		loc -= ROM_BASE;
		return loc;
	}
	
	public void elimShopClosures()
	{
		String[] locs = {"c398af", "c398c5", "c39812", "c39828", "c398e6", 
				         "c3984c", "c39862", "c39878", "c3988e", "c39907"};
		//instead of loading time of day, load 5pm
		String repl = "a9 22 00 ea";
		byte[] bb = strToBytes(repl);
		for(int i = 0; i < locs.length; i++)
		{
			int loc = addrToLoc(locs[i]);
			String check = "af 5d a0 7e";
			byte[] cc = strToBytes(check); 
			for(int j = 0; j < cc.length; j++)
				if(data[loc + j] != cc[j])
					System.err.println("Elim shop closures - incorrect data found");
			writeBytes(loc, bb);
		}
	}
	
	public void elimShipBuildCooldowns() 
	{
		String fx = "64 00 ea ea";  //simply store 0 to the number of days a ship will take to build
		int fxLoc = 534168 - 512;   //@82698
		byte[] fxb = strToBytes(fx);
		writeBytes(fxLoc, fxb);
	}

	private void testPortMarkets()
	{
		for(int i = 0; i < 100; i++)
		{
			int portLoc = 580761 - 512 + 37 * i;
			byte spGood = data[portLoc];
			short spPrice = readShort(portLoc - 2);
			byte mtype = data[portLoc + 7];
			int mtypeLoc = 725925 - 512 + 128 * mtype;
			if(spGood >= 0)
			{
				for(int j = 0; j < 9; j++)
					if(spGood == data[mtypeLoc + j + 110])
						System.err.println("Port #" + i + " is duplicate selling good #" + spGood + " in slot " + j + " of mktype " + mtype + " loc=" + portLoc);
			
				short buyPrice = readShort(mtypeLoc + 2 * spGood);
				buyPrice *= 3;
				buyPrice >>= 2;  //buyPrice = .75 * buyPrice
				if(spPrice < buyPrice)
					System.err.println("Port #" + i + " is selling its specialty good for " + spPrice + " and buying it for " + buyPrice);
			}
		}
		for(int i = 0; i < availGoodCount.length; i++)
			if(availGoodCount[i] == 0)
				System.err.println("Good #" + i + " is not available anywhere");
	}
	
	private void randomizeMarketTypes() //complete
	{
		//there are 13 market types
		int writeLoc = 725925 - 512;
		short[] lowPrice = {2, 16, 24, 36, 64, 120, 240};
		short[] hiPrice = {20, 30, 50, 100, 200, 400, 1200};
		byte success = 0;
		byte[][] specAt = {
				{0,1,3,6},
				{27,28,29,30,31,32,33,34,35,36,37,38,39,40,41},
				{7,8,9,10,11,12,13,14,15,16,21},
				{4,5,22,26},
				{2,17,18,19,20,23,24,25},
				{57,58,59,60,61,62,63,64,65},
				{44,46,47,48,49,50,51,55},
				{42,43,45,52,53,54,56},
				{66,67,68,69,70,71},
				{72,73,74,75,76,77,78,79,80},
				{81,82,83,85,92},
				{84,86,87,88,89,90,91,93},
				{94,95,96,97,98,99}};
		byte[] allGoods = new byte[46];
		byte[] inShops = new byte[13];
		int[] markets = new int[13];
		for(int i = 0; i < 13; i++)
		{
			int market = writeLoc;
			markets[i] = market;
			//buy prices
			for(int j = 0; j < 46; j++)
			{
				success = 0;
				for(int f = 0; f < 6; f++)
					if(UWNHRando.rand() > 0.5)  //bell curve of price ranges {(1,6,15,20,15,6,1) / 64}
						success++;
				short range = (short) (hiPrice[success] - lowPrice[success]);
				short price = (short) ((UWNHRando.rand() * range) + lowPrice[success]);
				writeShort(writeLoc, price);
				writeLoc += 2;
			}
			//# of available goods
			success = 5;
			for(int f = 0; f < 4; f++)
				if(UWNHRando.rand() > 0.5)  //5-9 goods available {(1,4,6,4,1) / 16}
					success++;
			inShops[i] = success;
			ArrayList<Byte> sold = new ArrayList<Byte>();
			//you cannot sell the port's specialty good at this port (or you will see the good listed twice with 2 different prices!)
			ArrayList<Byte> spec = new ArrayList<Byte>();
			for(int j = 0; j < specAt[i].length; j++)
			{
				int readAt = 580761 - 512;
				readAt += specAt[i][j] * 37;
				if(data[readAt] != -1)
				{
					spec.add(data[readAt]);
					allGoods[data[readAt]]++;
				}
			}
			
			for(int j = 0; j < 9; j++)
			{
				if(j < success)
				{
					byte good = 0;
					do
					{
						good = (byte) (UWNHRando.rand() * 46);
					} while(sold.contains(good) || spec.contains(good));
					sold.add(good);
					allGoods[good]++;
					//sell prices (should be greater than respective buy price by at least 30%)
					short sellPrice = readShort(market + (int) (2 * good));
					sellPrice = (short) ((UWNHRando.rand() + 1.3) * sellPrice);
					writeShort(writeLoc, sellPrice);
				}
				else
				{
					writeShort(writeLoc, 0);
					sold.add((byte) -1);
				}
				writeLoc += 2;
			}
			//available goods (ff = none)
			for(int j = 0; j < 9; j++)
			{
				data[writeLoc] = sold.get(j);
				writeLoc++;
			}
			//req'd industry to sell (/10)
			for(int j = 0; j < 9; j++)
			{
				byte good = sold.get(j);
				if(good == -1)  //none
					data[writeLoc] = -1;
				else if(good <= 16)  //food requires little industry (50-150)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 11) + 5);
				else if(good > 16 && good <= 25)  //textiles require more industry (150-300)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 16) + 15);
				else if(good > 25 && good <= 30)  //gems require a random amount of industry (80-400)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 33) + 8);
				else if(good > 30 && good <= 35)  //ores require more industry (200-450)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 26) + 20);
				else //all other finished goods require the most industry (300-750)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 46) + 30);
				writeLoc++;
			}
		}
		
		//ensure availability of all goods
		int selMarket = -1;
		ArrayList<Byte> availMkts = new ArrayList<Byte>();
		for(int i = 0; i < 13; i++)
			if(inShops[i] < 9)
				availMkts.add((byte) i);
		for(int i = 0; i < allGoods.length; i++)
		{
			if(allGoods[i] == 0)
			{
				byte m = -1;
				if(availMkts.size() == 0)
				{
					//remove the most available good
					byte hiGood = -1;
					byte hiAmt = 0;
					for(int j = 0; j < allGoods.length; j++)
					{
						if(allGoods[j] > hiAmt)
						{
							hiGood = (byte) j;
							hiAmt = allGoods[j];
						}
					}
					//from a market that has it
					for(int j = 0; j < 13; j++)
					{
						int readLoc = markets[j] + 2 * 46 + 2 * 9;
						boolean found = false;
						for(int k = 0; k < 9; k++)
						{
							if(data[readLoc] == hiGood)
							{
								m = (byte) j;
								found = true;
								
								for(int l = k; k < 9; l++)
								{
									int pLoc = markets[j] + 2 * 46 + 2 * l;
									data[pLoc + 1] = data[pLoc + 3];  //shift price
									data[pLoc] = data[pLoc + 2];
									data[readLoc] = data[readLoc + 1]; //shift good
									data[readLoc + 9] = data[readLoc + 10];  //shift reqd economy
									readLoc++;
								}
								inShops[m]--;
								availMkts.add(m);
								selMarket = availMkts.size() - 1;
							}
							if(found)
								break;
							readLoc++;
						}	
					}
					allGoods[hiGood]--;
				}
				else
				{
					//select an avail market and add it in
					int r = (int) (UWNHRando.rand() * availMkts.size());
					m = availMkts.get(r);
					selMarket = r;
				}
				int market = markets[m];
				byte good = (byte) i;
				short sellPrice = readShort(market + (int) (2 * good));
				sellPrice = (short) ((UWNHRando.rand() + 1.3) * sellPrice);
				writeLoc = market + 2 * 46 + 2 * inShops[m];
				writeShort(writeLoc, sellPrice);
				
				writeLoc = market + 2 * 46 + 2 * 9 + inShops[m];
				data[writeLoc] = good;
				
				writeLoc = market + 2 * 46 + 3 * 9 + inShops[m];
				
				if(good == -1)  //none
					data[writeLoc] = -1;
				else if(good <= 16)  //food requires little industry (50-150)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 11) + 5);
				else if(good > 16 && good <= 25)  //textiles require more industry (150-300)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 16) + 15);
				else if(good > 25 && good <= 30)  //gems require a random amount of industry (80-400)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 33) + 8);
				else if(good > 30 && good <= 35)  //ores require more industry (200-450)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 26) + 20);
				else //all other finished goods require the most industry (300-750)
					data[writeLoc] = (byte) ((UWNHRando.rand() * 46) + 30);
				
				inShops[m]++;
				if(inShops[m] == 9)
					availMkts.remove(selMarket);
				allGoods[i]++;
			}
		}
		availGoodCount = allGoods;
	}
	
	private void calcAvailGoods()
	{
		availGoodCount = new byte[46];
		//add all market goods
		int readAt = 725925 + 55 * 2 - 512;
		for(int i = 0; i < 13; i++)
		{
			for(int j = 0; j < 9; j++)
			{
				if(data[readAt + j] >= 0)
					availGoodCount[data[readAt + j]]++;
			}
			readAt += 128;
		}
		//add all specialty goods
		readAt = 580761 - 512;
		for(int i = 0; i < 100; i++)
		{
			if(data[readAt] >= 0)
			{
				availGoodCount[data[readAt]]++;
			}
			readAt += 37;
		}
	}

	private void randomizeMarketRates() 
	{
		int writeLoc = 580749 - 512;
		for(int i = 0; i < 100; i++)
		{
			// randomly sets each market sub-index from 0-100 
			// (50 is added to this value to get the final sub-index)
			// average market index is calculated from the 10 sub-indeces
			for(int j = 0; j < 10; j++)
				data[writeLoc + j] = (byte) (UWNHRando.rand() * 100);
			writeLoc += 37;
		}
		
	}

	private ArrayList<Byte> divideString(String in, boolean commod)
	{
		ArrayList<Integer> spc = new ArrayList<Integer>();
		int min  = -1;
		while(true)
		{
			int i1 = in.indexOf(' ', min + 1);
			int i2 = in.indexOf('-', min + 1);
			if(i2 == -1)
				min = i1;
			else if(i1 == -1)
				min = i2;
			else
				min = Math.min(i1,  i2);
			if(min == -1)
				break;
			spc.add(min);
		}
		spc.add(in.length());
		ArrayList<Byte> rv = new ArrayList<Byte>();
		int line = 16;
		if(commod)
			line = 8;
		int si = 0;
		for(; si < spc.size(); si++)
			if(spc.get(si) > line)
				break;
		int end = in.length();
		if(spc.size() > si)
			end = Math.min(spc.get(si - 1) + 1, in.length());
		else
			end = Math.min(end, line);
		if(commod && end == in.length())
		{
			rv.add((byte) 27);
			rv.add((byte) 77);
		}
		for(int j = 0; j < end; j++)
			rv.add((byte) in.charAt(j));
		if(end == in.length())
		{
			if(commod)
			{
				rv.add((byte) 27);
				rv.add((byte) 77);
			}
			rv.add((byte) 0);
			return rv;
		}
		int line2 = 30;
		if(commod)
			line2 = 14;
		int end2 = Math.min(in.length(), line2);
		rv.add((byte) 27);
		rv.add((byte) 68);
		for(int j = end; j < end2; j++)
			rv.add((byte) in.charAt(j));
		rv.add((byte) 27);
		rv.add((byte) 85);
		rv.add((byte) 0);
		return rv;
	}
	
	private void applyTheme(String[][][] theme, String[][][] origTheme)
	{
		romOrigTheme = origTheme;
		
		//Discovery names
		ArrayList<Integer> starts = new ArrayList<Integer>(100);
		ArrayList<Byte> cBlock = new ArrayList<Byte>();
		for(int i = 0; i < 99; i++)
		{
			starts.add(cBlock.size());
			String newName = theme[4][0][i];
			cBlock.addAll(divideString(newName, false));
		}
		int loc = 980597 - 512;
		int discLoc = loc;
		int endLoc = DATA_OVERFLOW + dataUsed;
		int discFree = -1;
		if(cBlock.size() > 1187) //we will need to move it to the data overflow
		{
			loc = endLoc;
			discFree = 1187;
			writeBytes(loc, cBlock);
			endLoc += cBlock.size();
			allocateData(cBlock.size());
		}
		else
			writeBytes(loc, cBlock);
		int ptrLoc = 980297 - 512;
		for(int i = 0; i < starts.size(); i++)
		{
			int dest = starts.get(i) + loc + ROM_BASE;
			write3Bytes(ptrLoc, dest);
			ptrLoc += 3;
		}
		
		//Commodity names
		starts.clear();
		cBlock.clear();
		for(int i = 0; i < 46; i++)
		{
			starts.add(cBlock.size());
			String newName = theme[5][0][i];
			cBlock.addAll(divideString(newName, true));
		}
		loc = 725393 - 512;
		int commodLoc = loc;
		//int endLoc = 2024224 - 512;
		int commodFree = -1;
		if(cBlock.size() > 532) //we will need to move it
		{
			if(cBlock.size() < discFree)
			{
				loc = discLoc;
				writeBytes(loc, cBlock);
				discFree -= cBlock.size();
				discLoc += cBlock.size();
			}
			else
			{
				loc = endLoc;
				commodFree = 532;
				writeBytes(loc, cBlock);
				endLoc += cBlock.size();
				allocateData(cBlock.size());
			}
		}
		else
			writeBytes(loc, cBlock);
		ptrLoc = 725255 - 512;
		for(int i = 0; i < starts.size(); i++)
		{
			int dest = starts.get(i) + loc + ROM_BASE;
			write3Bytes(ptrLoc, dest);
			ptrLoc += 3;
		}
		
		
		//Kingdom names
		starts.clear();
		cBlock.clear();
		char[] repLetters = new char[6];  //each kindgom gets a unique identifier
		for(int i = 0; i < 6; i++)
		{
			starts.add(cBlock.size());
			String newName = theme[0][0][i];
			int max = Math.min(16, newName.length());
			for(int j = 0; j < max; j++)
				cBlock.add((byte) newName.charAt(j));
			cBlock.add((byte) 0);
			int k = 0;
			int aa = -1;
			char rl = newName.charAt(k);
			boolean unique = false;
			if(i == 0)
				unique = true;
			while(!unique)
			{
				for(int j = i - 1; j >= 0; j--)
				{
					if(rl == repLetters[j])
					{
						k++;
						if(k < newName.length())
							rl = Character.toUpperCase(newName.charAt(k));
						else
						{
							aa++;
							rl = (char) ('A' + aa);
						}
						break;
					}
					if(j == 0)
					{
						unique = true;
						
					}
				}
			}
			repLetters[i] = rl;
			if(i == 5)  //add Piracy
			{
				starts.add(cBlock.size());
				newName = "Piracy";
				max = Math.min(16, newName.length());
				for(int j = 0; j < max; j++)
					cBlock.add((byte) newName.charAt(j));
				cBlock.add((byte) 0);
			}
		}
		int[] kItmLocs = {731127, 731148, 731169, 731190, 731211, 731232, 731252, 731272, 731292, 731312, 731332, 731352};
		for(int i = 0; i < kItmLocs.length; i++)
			data[kItmLocs[i] - 512] = (byte) repLetters[i % 6];
		loc = 723942 - 512;
		if(cBlock.size() > 51) //we will need to move it
		{
			if(cBlock.size() <= discFree)
			{
				loc = discLoc;
				writeBytes(loc, cBlock);
				discLoc += cBlock.size();
				discFree -= cBlock.size();
			}
			else
			{
				loc = endLoc;
				writeBytes(loc, cBlock);
				endLoc += cBlock.size();
			}
		}
		else
			writeBytes(loc, cBlock);
		ptrLoc = 723921 - 512;
		for(int i = 0; i < starts.size(); i++)
		{
			int dest = starts.get(i) + loc + ROM_BASE;
			write3Bytes(ptrLoc, dest);
			ptrLoc += 3;
		}
		
		//Kingdom adjectives
		starts.clear();
		cBlock.clear();
		for(int i = 6; i < 12; i++)
		{
			starts.add(cBlock.size());
			String newName = theme[0][0][i];
			int max = Math.min(16, newName.length());
			for(int j = 0; j < max; j++)
				cBlock.add((byte) newName.charAt(j));
			cBlock.add((byte) 0);
			if(i == 11)  //add Pirate
			{
				starts.add(cBlock.size());
				newName = "Pirate";
				max = Math.min(16, newName.length());
				for(int j = 0; j < max; j++)
					cBlock.add((byte) newName.charAt(j));
				cBlock.add((byte) 0);
			}
		}
		loc = 724014 - 512;
		if(cBlock.size() > 56) //we will need to move it
		{
			if(cBlock.size() <= discFree)
			{
				loc = discLoc;
				writeBytes(loc, cBlock);
				discLoc += cBlock.size();
				discLoc -= cBlock.size();
			}
			else
			{
				loc = endLoc;
				writeBytes(loc, cBlock);
				endLoc += cBlock.size();
				allocateData(cBlock.size());
			}
		}
		else
			writeBytes(loc, cBlock);
		ptrLoc = 723993 - 512;
		for(int i = 0; i < starts.size(); i++)
		{
			int dest = starts.get(i) + loc + ROM_BASE;
			write3Bytes(ptrLoc, dest);
			ptrLoc += 3;
		}
		
		//Area names (Northern Europe, Southern Europe, Iberian Peninsula, etc.)
		starts.clear();
		cBlock.clear();
		for(int i = 0; i < 13; i++)
		{
			starts.add(cBlock.size());
			String newName = theme[1][0][i];
			int max = Math.min(24, newName.length());
			for(int j = 0; j < max; j++)
				cBlock.add((byte) newName.charAt(j));
			cBlock.add((byte) 0);
		}
		loc = 761332 - 512;
		if(cBlock.size() > 165) //we will need to move it
		{
			if(cBlock.size() <= discFree)
			{
				loc = discLoc;
				writeBytes(loc, cBlock);
				discLoc += cBlock.size();
				discLoc -= cBlock.size();
			}
			else
			{
				loc = endLoc;
				writeBytes(loc, cBlock);
				endLoc += cBlock.size();
				allocateData(cBlock.size());
			}
		}
		else
			writeBytes(loc, cBlock);
		ptrLoc = 742782 - 512;  //we need to find this
		for(int i = 0; i < starts.size(); i++)
		{
			int dest = starts.get(i) + loc + ROM_BASE;
			write3Bytes(ptrLoc, dest);
			ptrLoc += 3;
		}
		
		//Port names
		loc = 577879 - 512;
		for(int i = 0; i < 100; i++)
		{
			String newName = theme[2][0][i];
			for(int j = 0; j < 16; j++)
				data[loc + j] = 0;
			int end = Math.min(newName.length(), 16);
			for(int j = 0; j < end; j++)
				data[loc + j] = (byte) newName.charAt(j);
			loc += 22;
		}
		
		//port Market, Shipyard, and Specialty good
		loc = 580761 - 512;
		int mktLocs = 725925 - 512;  //for pricing specialty goods
		for(int i = 0; i < 100; i++)
		{
			//change market type first (so specialty good isn't duplicate)
			int mTypeLoc = loc + 7;
			String ss = theme[2][1][i];
			int val = Integer.parseInt(ss.substring(0, ss.indexOf('.')));
			if(val == 0)
				val = (int) (UWNHRando.rand() * 13);  //market type
			else 
				val--;
			int mtype = val;
			data[mTypeLoc] = (byte) (val & 255);
			/*System.err.println("Port #" + i + " mktType=" + mtype);// + Arrays.toString(Arrays.copyOfRange(data, mktLocs - 512 + 110, mktLocs - 512 + 119)));
			for(int j = 0; j < 9; j++)
			{
				byte good = data[mktLocs + 110 + 128 * mtype + j];
				short buy = readShort(mktLocs + 128 * mtype + 2 * good);
				short sell = readShort(mktLocs + 128 * mtype + 92 + 2 * j);
				System.err.println("good= " + good + " buy=" + buy + " sell=" + sell);
			}*/
			
			
			//then specialty good
			boolean sgChanged = false;
			int sellPriceLoc = loc - 2;
			int ecoReqLoc = loc + 1;
			ss = theme[2][3][i];
			val = Integer.parseInt(ss.substring(0, ss.indexOf('.')));
			if(val == 0 || !specialtyGoodAllowed(val - 1, mtype))
			{
				do
				{
					val = (int) (UWNHRando.rand() * 46);  //specialty good
				} while(!specialtyGoodAllowed(val, mtype));
			}
			else if(val > 0)
				val--;
			int sg = val;
			//System.err.print("spcGood=" + sg);
			String so = origTheme[2][3][i];
			int oldVal = Integer.parseInt(so);
			if(oldVal > 0)
				oldVal--;
			if(oldVal != sg)
				sgChanged = true;
			if(availGoodCount == null)
				calcAvailGoods();
			if(oldVal >= 0)
			{
				if(availGoodCount[oldVal] > 1)  
				{	//note: a specialty good sold in a market it's already sold at will put the count at at least 2
					data[loc] = (byte) (sg & 255);
					//System.err.print(" [" + sg + " put at " + loc + "]");
					availGoodCount[sg]++;
					availGoodCount[oldVal]--;
				}
				else
				{
					//don't change the good
					sgChanged = false;
					//System.err.println("spcGood=" + oldVal);
					sg = oldVal;
				}
				//but see if the price needs changing
				int priceLoc = mktLocs + (128 * mtype) + (2 * sg);
				int buyPrice = readShort(priceLoc);
				int sellPrice = readShort(sellPriceLoc);
				if(sellPrice < buyPrice)  //we still need to update the price
					sgChanged = true;
				
			}
			else
			{
				sgChanged = false;
				//System.err.println("[no spclty good]");
			}
			//System.err.println("[dl=" + loc + " val=" + data[loc] + "]");
			//loc += 7;
			
			loc = mTypeLoc + 1;
			ss = theme[2][2][i];
			val = Integer.parseInt(ss.substring(0, ss.indexOf('.')));
			if(val == 0)
				val = (int) (UWNHRando.rand() * 11);  //shipyard type
			else
				val--;
			data[loc] = (byte) (val & 255);
			
			//re-price the specialty good to 75% to 225% of buy price, skewed toward the center
			if(sgChanged)
			{
				int priceLoc = mktLocs + (128 * mtype) + (2 * sg);
				int buyPrice = readShort(priceLoc);
				double muMax = 0.25;
				for(int k = 0; k < 5; k++)
					if(UWNHRando.rand() > 0.5)
						muMax += 0.25;
				double markup = (UWNHRando.rand() * muMax) + 0.75;
				int sellPrice = (int) (buyPrice * markup);
				writeShort(sellPriceLoc, sellPrice);
				//req'd economy to sell good is based on good type (lower req'd economy because its a port specialty)
				byte reqE = 0;
				if(sg <= 9)  //specialty food
					reqE = 5;
				else if(sg <= 16)  //food
					reqE = 0;
				else if(sg <= 25)  //textiles
					reqE = 12;
				else if(sg <= 35)  //jewelry
					reqE = 28;
				else if(sg <= 39)  //luxuries
					reqE = 35;
				else               //other
					reqE = 25;
				data[ecoReqLoc] = reqE;
				//System.out.println("  sgPrice=" + sellPrice);
			}
			/*short buy = readShort(mktLocs + 128 * mtype + 2 * sg);
			short sell = readShort(sellPriceLoc);
			System.err.println(" buy=" + buy + " sell=" + sell);*/
			loc += 29;
		}
		
		//culture
		loc = 735038 - 512;
		for(int i = 0; i < 100; i++)
		{
			String ss = theme[2][4][i];
			int val = Integer.parseInt(ss.substring(0, ss.indexOf('.')));
			val--;
			data[loc + i] = (byte) (val & 255);
		}
		
		//Supply Ports
		loc = 580079 - 512;
		for(int i = 0; i < 30; i++)
		{
			String newName = theme[3][0][i];
			for(int j = 0; j < 16; j++)
				data[loc + j] = 0;
			int end = Math.min(newName.length(), 16);
			for(int j = 0; j < end; j++)
				data[loc + j] = (byte) newName.charAt(j);
			loc += 22;
		}
		
		
	}
	
	private boolean specialtyGoodAllowed(int good, int mktType) 
	{
		if(good < 0)
			return false;
		int readLoc = 726035 + 128 * mktType - 512;
		for(int i = 0; i < 9; i++)
			if(data[readLoc + i] == good)
				return false;
		return true;
	}

	private String bytesToText(ArrayList<Byte> bytes)
	{
		String str = "";
		for(int i = 0; i < bytes.size(); i++)
		{
			byte bb = bytes.get(i);
			str += (char) (bb);
		}
		return str;
	}
	
	public GameMap.PortData[] getOrigPortData()  //call this before applying themes
	{
		int loc = 735038 - 512;  //cultural data
		int xyloc = 577875 - 512;
		GameMap gm = new GameMap();
		//Port[] ports  = UWNHRando.collectPorts(this, false);
		GameMap.PortData[] rv = new GameMap.PortData[100];
		int[] caps = {0,1,2,8,29,33,77};
		for(int i = 0; i < rv.length; i++)
		{
			byte c = data[loc + i];
			boolean capital = false;
			if(Arrays.binarySearch(caps, i) >= 0)
				capital = true;
			rv[i] = gm.new PortData(-1, capital, null);
			short x = readShort(xyloc);
			xyloc += 2;
			short y = readShort(xyloc);
			rv[i].setXY(x, y);
			xyloc += 20;
			rv[i].setOrigIndex(i);
			rv[i].origCulture = c;
		}
		return rv;
	}

	public String[][][] getOrigThemeData() 
	{
		String[][][] rv = new String[6][][];
		rv[0] = new String[1][12];
		//Theme 0a: Kingdom names and adjectives
		//int loc = 723942 - 512;
		
		ArrayList<Byte> curr = new ArrayList<Byte>();
		int[] locs = {723942 - 512, 724014 - 512};
		for(int i = 0; i < 2; i++)
		{
			int zc = 0;
			while(zc < 6)
			{
				if(data[locs[i]] == 0)
				{
					rv[0][0][zc + 6 * i] = bytesToText(curr);
					zc++;
					curr.clear();
				}
				else
					curr.add(data[locs[i]]);
				locs[i]++;
			}
		}
		//Theme 1: Area names
		rv[1] = new String[1][13];
		int loc = 761332 - 512;
		int zc = 0;
		while(zc < 13)
		{
			if(data[loc] == 0)
			{
				rv[1][0][zc] = bytesToText(curr);
				zc++;
				curr.clear();
			}
			else
				curr.add(data[loc]);
			loc++;
		}
		//Theme 2: Port Names
		rv[2] = new String[5][100];
		loc = 577879 - 512;
		zc = 0;
		while(zc < 100)
		{
			if(data[loc] == 0)
			{
				rv[2][0][zc] = bytesToText(curr);
				zc++;
				loc -= curr.size();
				loc += 22;
				curr.clear();
			}
			else
			{
				curr.add(data[loc]);
				loc++;
			}
		}
		//market type, sy type, speclty good, culture type
		loc = 580761 - 512;
		for(int i = 0; i < 100; i++)
		{
			int v = data[loc];
			//System.out.print(i + ":" + v + ",");
			if(v < 0)
				rv[2][3][i] = String.valueOf(v);  //none (ff)
			else
				rv[2][3][i] = String.valueOf(v + 1);  //specialty good, 1 indexed (0 is reserved for random)
			loc += 7;
			v = data[loc];
			rv[2][1][i] = String.valueOf(v + 1);
			loc++;
			v = data[loc];
			rv[2][2][i] = String.valueOf(v + 1);
			loc += 29;
		}
		loc = 735038 - 512;
		for(int i = 0; i < 100; i++)
		{
			int v = data[loc];
			rv[2][4][i] = String.valueOf(v + 1);
			loc++;
		}
		//Theme 3: Supply Port Names
		rv[3] = new String[1][30];
		loc = 580079 - 512;
		zc = 0;
		while(zc < 30)
		{
			if(data[loc] == 0)
			{
				rv[3][0][zc] = bytesToText(curr);
				zc++;
				loc -= curr.size();
				loc += 22;
				curr.clear();
			}
			else
			{
				curr.add(data[loc]);
				loc++;
			}
		}
		//Theme 4: Discovery Names
		rv[4] = new String[1][99];
		loc = 980597 - 512;
		zc = 0;
		while(zc < 99)
		{
			if(data[loc] == 0)
			{
				for(int i = 0; i < curr.size(); i++)
				{
					if(curr.get(i) == 27)  //remove all instances of 1b and what follows it
					{
						curr.remove(i);
						curr.remove(i);
						i--;
					}
				}
				rv[4][0][zc] = bytesToText(curr);
				zc++;
				curr.clear();
			}
			else
				curr.add(data[loc]);
			loc++;
		}
		//Theme 5: Commodity names
		rv[5] = new String[1][46];
		loc = 725393 - 512;
		zc = 0;
		while(zc < 46)
		{
			if(data[loc] == 0)
			{
				for(int i = 0; i < curr.size(); i++)
				{
					if(curr.get(i) == 27)  //remove all instances of 1b and what follows it
					{
						curr.remove(i);
						curr.remove(i);
						i--;
					}
				}
				rv[5][0][zc] = bytesToText(curr);
				zc++;
				curr.clear();
			}
			else
				curr.add(data[loc]);
			loc++;
		}
		return rv;
	}
}

class Compressor
{
	short a;
	short x;
	short y;
	short data; //f89
	short shiftedData;  //f87
	short bytesLeft; //f8b
	short bytesToShift; //f8d
	short patternLen; //f8f
	byte pBits; //f91
	ArrayList<Short> stack;
	SNESRom rom;
	int breaks;
	int initSrc;
	int dataSize;
	ArrayList<Byte> controlMarking;
	ArrayList<Byte> compMarking;
	
	ArrayList<Short> decompPairs;
	ArrayList<Short> compPairs;
	int[] compDeltas;
	
	Compressor(SNESRom sr)
	{
		rom = sr;
		deltaIndeces = null;
	}
	
	
	


	public ArrayList<Byte> gameDecompress(int dataLoc)
	{
		return decompress(dataLoc - 512, rom.data);
	}
	
	public ArrayList<Byte> decompress(int dataLoc, byte[] srcData)  //dataloc is the byte position in the .smc file
	{
		stack = new ArrayList<Short>();
		controlMarking = new ArrayList<Byte>();
		breaks = 1;
		ArrayList<Byte> dest = new ArrayList<Byte>();
		decompPairs = new ArrayList<Short>();
		//int loc = dataLoc;
		int src = dataLoc;  //shave off the header
		initSrc = src;
		bytesLeft = 0;
		bytesToShift = 0;
		cursor = 0;
		data = (short) ((srcData[src + 1] << 8) | (srcData[src] & 255));
		shiftedData = data;
		src += 2;
		controlMarking.add((byte) 0);
		controlMarking.add((byte) 0);
		x = 0;
		int a0ef = 41711 - 512;
		int a16e = a0ef + 127;  //shift amt
		int a12f = a0ef + 64;   //shift amt 2
		int a176 = a0ef + 135;  //pattern dist
		int a236 = a0ef + 327;
		int a23f = a0ef + 336;
		int a244 = a0ef + 341;
		int c0, c1;
		
		while(true)
		{
			x--;
			if(x < 0)  //read in $f91 (control bits)
			{
				pBits = srcData[src];
				src++;
				controlMarking.add((byte) 2);
				x = 7;
			}
			c0 = cursor;
			boolean copy = topBit(pBits, 1);
			pBits <<= 1;
			if(copy)
			{
				byte c = srcData[src];
				dest.add(c);
				src++;
				controlMarking.add((byte) 1);
			}
			else
			{
				pushStack(x);
				a = shiftedData;
				//System.out.println("a has " + UWNHRando.viewByte((byte) (a >> 8)) + " " + UWNHRando.viewByte((byte) (a & 255)));
				if(topBit(a, 2))  //bmi
				{
					pushStack((short) 1);  //copy 1 byte
				}
				else
				{
					a >>= 1;   //6 bits (max 63; top bit is 0 and >> 1)
					xba();
					y = (short) (a & 255);  
					if(y != 0)
					{
						if(y < 8)
						{
							xba();  //max 63
							x = rom.data[a16e + y]; //{4,4,8,8,12,12,12,12}
							a >>= (x / 2);  //maxes:{1ff,1ff,7f,7f,1f,1f,1f,1f}
						}
						else
						{
							x = rom.data[a0ef + y];  //values 2-15
							a = x;
						}
						//System.out.println("y was " + y + " and a was " + a);
						pushStack(a);  //number of byes to copy
						x = rom.data[a12f + y]; //values {18-4}
						src = shift16(src, srcData);  //2-12 bits
						
					}
					else
					{
						xba();
						a >>= 1;
						a |= 128;
						if(a == 255)  //end
						{
							dataSize = src - initSrc;
							return dest;
						}
						else
						{
							//System.out.println("y was 0 and a was " + a);
							pushStack(a);  //number of bytes to copy (128-254 bytes}
							x = rom.data[a12f + y]; //always 26 because y is 0
							src = shift16(src, srcData);  //13 bits
						}
					}
				}
				//System.out.println("a has " + UWNHRando.viewByte((byte) (a >> 8)) + " " + UWNHRando.viewByte((byte) (a & 255)));
				a <<= 1;  //either bytes to copy OR shiftedData
				//System.out.println("Testing " + UWNHRando.viewByte((byte) (a >> 8)) + " " + UWNHRando.viewByte((byte) (a & 255)) + " against 0x6000");
				c1 = cursor;
				if(a < 24576 && a >= 0)   //cmp 6000
				{
					a <<= 1;
					xba();
					y = (short) (a & 255);  //max=bf (191)
					//System.out.println("Y was " + y);
					x = rom.data[a176 + y];   //pattern dist {0-7f}
					a = y;
					a >>= 4;
					y = (short) (a & 255);  //max=b (11)
					a = x;
					x = rom.data[a236 + y];   //shift bits (/2) {0e,0e,10,12x3,14x6}
				}
				else
				{
					//System.err.println("Entered large decompression;a=" + a);
					boolean carry1 = false;
					if(a < 0)
						carry1 = true;
					a <<= 1;
					rol(3, carry1);
					a &= 7;  //top 3 bits
					y = (short) (a & 255);  //max 7
					a = shiftedData;
					a &= 4095;
					a |= 4096;  //13 bits
					x = rom.data[a23f + y]; //{20,20,20,10,8,6,4,2}
					a >>= (x / 2);   //pattern dist from end
					x = rom.data[a244 + y];  //{6,4,2,22,24,26,28,30}
				}
				//System.out.println("Pattern dist back is " + a);
				patternLen = a;
				src = shift16(src, srcData);
				int st = dest.size();
				//a = y;
				st -= (patternLen + 1);
				//x = a;
				a = popStack();
				decompPairs.add(a);
				decompPairs.add(patternLen);
				//System.out.println("Output length " + a + " at cursor " + c0 + " and dist " + patternLen + " at cursor " + c1);
				//if(c1 == 185)
					//System.out.println();
				mvn(dest, st);
				x = popStack();
			}
		}
	}
	
	public ArrayList<Byte> decompress2(int dataLoc, byte[] srcData)
	{
		stack = new ArrayList<Short>();
		controlMarking = new ArrayList<Byte>();
		breaks = 1;
		ArrayList<Byte> dest = new ArrayList<Byte>();
		decompPairs = new ArrayList<Short>();
		//int loc = dataLoc;
		int src = dataLoc;  //shave off the header
		initSrc = src;
		bytesLeft = 0;
		bytesToShift = 0;
		cursor = 0;
		//data = (short) ((srcData[src + 1] << 8) | (srcData[src] & 255));
		shiftedData = data;
		//src += 2;
		/*controlMarking.add((byte) 0);
		controlMarking.add((byte) 0);*/
		x = 0;
		int a0ef = 41711 - 512;
		int a16e = a0ef + 127;  //shift amt
		int a12f = a0ef + 64;   //shift amt 2
		int a176 = a0ef + 135;  //pattern dist
		int a236 = a0ef + 327;
		int a23f = a0ef + 336;
		int a244 = a0ef + 341;
		while(true)
		{
			x -= 2;
			if(x < 0)  //read in $f91 (control bits)
			{
				pBits = srcData[src];
				src++;
				controlMarking.add((byte) 2);
				x = 6;
			}
			//c0 = cursor;
			//boolean copy = topBit(pBits, 1);
			byte mode = (byte) ((pBits >> 6) & 3);
			pBits <<= 2;
			if(mode == 0)
			{
				byte c = srcData[src];
				dest.add(c);
				src++;
				controlMarking.add((byte) 1);
			}
			else
			{
				byte len = 0;
				short dist = 0;
				len = srcData[src];
				src++;
				if(len == -1)
				{
					dataSize = src - initSrc;
					return dest;
				}
				if(mode == 2)
				{
					dist = (short) (srcData[src] & 255);
					src++;
				}
				if(mode == 3)
				{
					dist = (byte) (len & 15);
					len = (byte) ((len >> 4) & 15);
				}
				//patternLen = a;
				//src = shift16(src, srcData);
				int st = dest.size();
				//a = y;
				st -= (dist + 1);
				//x = a;
				a = (short) (len & 255);
				decompPairs.add(a);
				decompPairs.add((short) dist);
				//System.out.println("Output length " + a + " at cursor " + c0 + " and dist " + patternLen + " at cursor " + c1);
				//if(c1 == 185)
					//System.out.println();
				mvn(dest, st);
				//x = popStack();
			}
		}
	}
	
	private int shift16(int ds, byte[] srcData)
	{
		int a0d5 = 41685 - 512;  //a0d5 = bit mask for lowest x/2 bits set
		int amt = x / 2;
		bytesToShift = (short) amt;
		//System.out.println("Shifting off " + bytesToShift + " bits");
		a = shiftedData;
		a <<= amt;
		cursor += amt;
		shiftedData = a;
		x = (short) (bytesLeft * 2);
		if(bytesLeft < bytesToShift)
		{
			a = (short) (bytesToShift - bytesLeft);
			bytesToShift = a;
			a = data;
			short andMe = (short) ((rom.data[a0d5 + x + 1] << 8) | (rom.data[a0d5 + x] & 255));
			a &= andMe;
			x = (short) (bytesToShift * 2);
			a <<= bytesToShift;
			shiftedData |= a;
			if((shiftedData & -4) != 508)  //7x0 + 7x1; if you find this the algorithm is done so no need to grab more bytes
			{
				a = (short) ((srcData[ds + 1] << 8) | (srcData[ds] & 255));
				data = a;
				controlMarking.add((byte) 0);
				controlMarking.add((byte) 0);
			}
			else
			{
				a = 0;   //pretend it's unused data
				controlMarking.add((byte) 3);
				controlMarking.add((byte) 3);
			}
			ds += 2;
			
			bytesLeft = 16;
			x = 32;
		}
		bytesLeft -= bytesToShift;
		a = (short) (bytesLeft * 2);
		x = a;
		a = data;
		a >>= bytesLeft;
		x = (short) (bytesToShift * 2);
		short andMe = (short) ((rom.data[a0d5 + x + 1] << 8) | (rom.data[a0d5 + x] & 255));
		a &= andMe;
		a |= shiftedData;
		shiftedData = a;
		return ds;
	}
	
	private boolean topBit(int val, int size)
	{
		
		int[] tests = {128,32768};
		int v = val & tests[size - 1];
		return v != 0;
	}
	
	private void pushStack(short bb)
	{
		stack.add(bb);
	}
	
	private short popStack()
	{
		short rv = stack.get(stack.size() - 1);
		stack.remove(stack.size() - 1);
		return rv;
	}
	
	private void xba()  //exchange bytes in a
	{
		int a1 = a & 255;
		int a2 = (a >> 8) & 255;
		a = (short) ((a1 << 8) | a2);
	}
	
	private void rol(int nTimes, boolean carrySet)  //rotate left preserves the bits that are rolled off
	{
		byte carry = 0;
		byte carry2 = 0;
		if(carrySet)
			carry2 = 1;
		for(int i = 0; i < nTimes; i++)
		{
			//a |= carry;
			if(topBit(a, 2))
				carry = 1;
			else
				carry = 0;
			a <<= 1;
			a |= carry2;
			carry2 = carry;
		}
	}
	
	
	
	private void mvn(ArrayList<Byte> dest, int start)
	{
		String aa = Integer.toHexString(a);
		String xx = Integer.toHexString(start);
		String yy = Integer.toHexString(dest.size());
		//String info = "break #" + breaks + "  mvn a=" + aa + " x=" + xx + " y=" + yy;
		//System.out.println(info);
		
		for(int i = a; i >= 0; i--)
		{
			dest.add(dest.get(start));
			start++;
		}
		breaks++;
		
	}
	
	public ArrayList<Byte> compressBytes2(byte[] in)
	{
		int c = 0;  //consumed
		
		ArrayList<Short> patternBytes = new ArrayList<Short>();
		ArrayList<Byte> controlBytes = new ArrayList<Byte>();
		ArrayList<Byte> copyBytes = new ArrayList<Byte>();
		controlBytes.add((byte) 0);
		copyBytes.add(in[c]);
		c++;
		while(c < in.length)
		{
			int ptnPos = 0;
			int ptnLen = 0;
			int bestPtnPos = 0;
			int bestPtnLen = 0;
			for(int i = c - 1; i >= 0; i--)
			{
				if(i == c - 256)
					break;
				if(in[i] == in[c]) //look back finding matches
				{
					ptnPos = i;
					ptnLen = 0;
					while(in[ptnPos + ptnLen] == in[c + ptnLen])
					{
					   ptnLen++;
					   if(c + ptnLen >= in.length)
						   break;
					}
					if(ptnLen > bestPtnLen)
					{
						bestPtnPos = c - ptnPos - 1;
						bestPtnLen = ptnLen;
					}
				}
			}
			if(bestPtnLen > 0)
			{
			    //put pattern
				if(bestPtnPos == 0)
				{
					controlBytes.add((byte) 1);
					patternBytes.add((short) bestPtnLen);
				}
				else if(bestPtnPos < 16 && bestPtnLen < 16)
				{
					controlBytes.add((byte) 3);
					short hbyte = (short) (bestPtnPos | (bestPtnLen << 4));
					patternBytes.add(hbyte);
				}
				else
				{
					controlBytes.add((byte) 2);
					patternBytes.add((short) bestPtnLen);
					patternBytes.add((short) bestPtnPos);
				}
			    c += bestPtnLen;
			}
			else  //just dump the byte
			{
				controlBytes.add((byte) 0);
				copyBytes.add(in[c]);
				c++;
			}
		}
		patternBytes.add((short) 255);
		controlBytes.add((byte) 1);

		ArrayList<Byte> compPatterns = new ArrayList<Byte>();
		ArrayList<Byte> types = new ArrayList<Byte>();
		//compPatterns.add((byte) 0);
		//compPatterns.add((byte) 0);
		//types.add((byte) 0);  //do not prepend 2 stream bytes
		//types.add((byte) 0);
		types.add((byte) 2);
		//ArrayList<ArrayList<Byte>> rv = generatePatterns(patternBytes);
		ArrayList<Byte> out = new ArrayList<Byte>();
		//then weave them together
		byte control = 0;
		byte cshift = 6;
		int lastControl = 0;
		int nPatternBytes = 0;  //change to 0?
		int lastPattern = 0;
		width = 16;
		rem = 0;
		cursor = 0;
		int cleft = 0;
		writeByte = 0;
		//init - shift 2
		//out.add((byte) 0);
		//out.add((byte) 0);
		out.add(control);
		for(int i = 0; i < controlBytes.size(); i++)
		{
			//ArrayList<Byte> ptn = rv.get(i);
			byte ctrl = controlBytes.get(i);
			if(ctrl == 0)
			{
				//control |= (1 << cshift);
				out.add(copyBytes.remove(0));
				types.add((byte) 1);
			}
			else
			{
				control |= (ctrl << cshift);
				short len = patternBytes.get(lastPattern);
				short dist = 0;
				lastPattern++;
				if(len != 255 && ctrl == 2)
				{
					dist = patternBytes.get(lastPattern);
					lastPattern++;
				}
				//if(dist == 0)
				//{
				types.add((byte) 0);
				if(len == 255)
					out.add((byte) -1);
				else if(ctrl != 3)
					out.add((byte) (len - 1));
				else
					out.add((byte) (len - 16));
				//}
				if(ctrl == 2)
				{
					types.add((byte) 0);
					//out.add((byte) 0);
					out.add((byte) dist);
				}
				/*int dc = cursor;
					//compPatterns = generatePatterns(compPatterns, len, dist);
					dc -= cursor;
					cleft += dc;
					while(cleft < 0)  //add 2 bytes of stream
					{
						types.add((byte) 0);
						out.add((byte) 0);
						out.add((byte) 0);types.add((byte) 0);
						types.add((byte) 0);
						out.add((byte) 0);
						out.add((byte) 0);
						cleft += 16;
					}*/
				/*for(int j = nPatternBytes; j < compPatterns.size(); j++)
					{
						types.add((byte) 0);
						out.add((byte) 0);
						//added++;
					}*/
				//nPatternBytes = compPatterns.size();
				/*if((added & 1) == 1)
					{
						types.add((byte) 0);
						out.add((byte) 0);
					}*/
			}
			cshift -= 2;
			if(cshift < 0)
			{
				out.set(lastControl, control);
				control = 0;
				lastControl = out.size();
				out.add((byte) 0);
				types.add((byte) 2);
				cshift = 6;
			}
		}
		if(cshift == 6) //remove the last control byte (as it is unused)
		{
			for(int i = types.size() - 1; i >= 0; i--)
			{
				if(types.get(i) == 2)
				{
					types.remove(i);
					out.remove(i);
					break;
				}
			}
		}
		else
			out.set(lastControl, control);
		//finally insert the compressed pattern
		/*if((compPatterns.size() & 1) == 1)  //if patterns size is odd output a 0
			{
				compPatterns.add((byte) 0);
				types.add((byte) 0);
				out.add((byte) 0);
			}*/
		//System.out.print("stream:");
		//for(int i = 0; i < compPatterns.size(); i++)
		//System.out.print(UWNHRando.viewByte(compPatterns.get(i)) + ",");
		//System.out.println();
		/*int added = 0;
			for(int i = 0; i < types.size(); i++)   //ensure that stream bytes are delivered 2 at a time
			{
				if(types.get(i) == 0)
				{
					added++;
				}
				else
				{
					if((added & 1) == 1)
					{
						types.add(i, (byte) 0);
						out.add(i, (byte) 0);
					}
					added = 0;
				}
			}*/

		/*byte remove = 1;
			for(int i = 0; i < types.size(); i++)
			{
				if(types.get(i) == 0)
				{
					if(compPatterns.size() == 0)
					{
						//while(i < out.size())
						//out.remove(i);
						types.set(i,(byte) 3);  //it's a future byte
						//i--;
						continue;
						//break;
					}
					byte b = compPatterns.remove(remove);
					out.set(i, b);
					remove ^= 1;
				}
			}*/
		compMarking = types;
		return out;
		
		//return null;
	}
	
	private void viewC3Data(byte[] in, int width)
	{
		for(int yy = 0; yy < width; yy++)
		{
			for(int xx = 0; xx < width; xx++)
			{
				String s = Integer.toHexString(in[yy * width + xx]);
				if(s.length() == 1)
					s = " " + s;
				System.out.print(s + ",");
			}
			System.out.println();
		}
	}
	
	public ArrayList<Byte> compressBytes3(byte[] in, int width, boolean verbose)  //rectangular RLE for 12x12 blocks and/or 1024x1024 boxes
	{
		int cursor = 0;
		boolean[] used = new boolean[in.length];
		byte control = 0;
		byte shift = 7;
		int lastControl = 0;
		byte oldB = 0;
		ArrayList<Byte> pairs = new ArrayList<Byte>();
		pairs.add(control);
		
		//viewC3Data(in, width);
				
		//ArrayList<Byte> controls = new ArrayList<Byte>();
		int assumedBox = -1;
		while(cursor < in.length)
		{
			while(used[cursor] == true)
			{
				cursor++;
				if(cursor == in.length)
					break;
			}
			if(cursor == in.length)
				break;
			byte b = in[cursor];
			//if(b == 10)
				//System.out.println();
			used[cursor] = true;
			//int boxCursor = cursor;
			int x =  (cursor % width);
			int y =  (cursor / width);
			y *= width;
			int dx = 1;
			int dy = width;
			boolean growX = true;
			boolean growY = true;
			while(true)
			{
				if(x + dx >= width)
				{
					growX = false;
					dx--;
				}
				if(y + dy >= in.length)
					growY = false;
				if(growX)
				{
					for(int yy = 0; yy < dy; yy += width)
					{
						int loc = cursor + dx + yy;
						if(in[loc] != b || used[loc])
						{
							growX = false;
							for(int yyb = yy - width; yyb >= 0; yyb -= width)
								used[cursor + dx + yyb] = false;
							dx--;
							break;
						}
						used[loc] = true;
					}
				}
				if(growY)
				{
					for(int xx = 0; xx <= dx; xx++)
					{
						int loc = cursor + dy + xx;
						if(in[loc] != b || used[loc])
						{
							growY = false;
							for(int xxb = xx - 1; xxb >= 0; xxb--)
								used[cursor + dy + xxb] = false;
							//dy -= width;
							break;
						}
						used[loc] = true;
					}
				}
				if(!growX && !growY)
					break;
				if(growX)
					dx++;
				if(growY)
					dy += width;
			}
			dx++;
			dy /= width;
			byte box = (byte) ((dy << 4) | dx);
			if(verbose)
				System.out.println(cursor + ": Found box of size " + dx + " by " + dy + " val " + b);
			if(cursor == 0 || oldB != b || box == 17 || assumedBox != -1)
			{
				pairs.add(b);
				shift--;
				if(shift < 0)
				{
					pairs.set(lastControl, control);
					control = 0;
					pairs.add(control);
					lastControl = pairs.size() - 1;
					shift = 7;
				}
				assumedBox = cursor;
			}
			if(box > 17 || box < 0)
			{
				pairs.add(box);
				control |= 1 << shift;
				shift--;
				if(shift < 0)
				{
					pairs.set(lastControl, control);
					control = 0;
					pairs.add(control);
					lastControl = pairs.size() - 1;
					shift = 7;
				}
				assumedBox = -1;
			}
			//else
				//lastPlacedBox = false;
			oldB = b;
		}
		//terminator - this will not be necessary
		/*if(shift == 0)
			pairs.add((byte) -128);
		else
		{
			control |= 1 << shift;
			pairs.set(lastControl, control);
		}
		pairs.add((byte) -1);*/
		if(shift != 7)
			pairs.set(lastControl, control);
		else
			pairs.remove(lastControl);
		if(verbose)
			System.out.println("compressed output = " + pairs.toString());
		return pairs;
	}
	
	public ArrayList<Byte> decompress3(int dataLoc, byte[] srcData, int width, int outputSize, byte[] origBlock, boolean verbose)
	{
		ArrayList<Byte> dest = new ArrayList<Byte>();
		int src = dataLoc;
		initSrc = dataLoc;
		byte[] out = new byte[outputSize];
		byte[] used = new byte[outputSize >> 3];
		//byte usedShift = 7;
		int cursor = 0;
		int boxCursor = 0;
		int usedCursor = 0;
		x = 0;
		byte pBits = 0;
		byte copyMe = 0;
		boolean carry = false;
		while(true)
		{
			x--;
			if(x < 0)
			{
				pBits = srcData[src];
				src++;
				x = 7;
			}
			boolean box = topBit(pBits, 1);
			pBits <<= 1;
			if(carry)
				pBits |= 1;  //this is taken care of with a rol
			carry = box;
			pushStack(x);
			if(!box)
			{
				copyMe = srcData[src];
				src++;
				out[cursor] = copyMe;
				boxCursor = cursor;
				a = (short) cursor;
				x = a;
				a >>= 3;
				usedCursor = a;
				a = x;
				a = (short) (a & 7);
				used[usedCursor] |= 1 << a;
				if(verbose)
					System.out.println("Output " + copyMe + " at location " + cursor);
			}
			else
			{
				if((pBits & 1) == 0)  //if you previously output a non-box
					cursor = boxCursor;
				short cursor2 = (short) cursor;
				short putCursor = cursor2;
				//while(cursor2 > width)
					//cursor2 -= width;
				a = srcData[src];
				a = (short) ((a >> 4) & 15);
				//a *= width;
				y = a;
				int boxh = y;
				int boxw = 0;
				while(y > 0)
				{
					a = (short) (srcData[src] & 15);
					boxw = a;
					a += cursor2;
					
					//x = (short) (a & 15);    //lda and 15 tax
					while(a > cursor2)
					{
						a--;
						out[a] = copyMe;
						pushStack(a);
						x = a;
						a >>= 3;
						usedCursor = a;
						a = x;
						a = (short) (a & 7);
						used[usedCursor] |= 1 << a;
						a = popStack();
					}
					cursor2 += width;
					y--;
				}
				if(verbose)
					System.out.println("Put box val " + copyMe + " w=" + boxw + " h=" + boxh + " at " + putCursor);
				src++;
			}
			//if(copyMe == 1)
				//System.out.println();
			int oldCursor = cursor;
			while(cursor < outputSize)
			{
				a = (short) cursor;
				x = a;
				a >>= 3;
				usedCursor = a;
				a = x;
				a = (short) (a & 7);
				if((used[usedCursor] & (1 << a)) != 0)  //tax shift ramp ldx usedCursor, ora used,x OR and used,x beq start cursor++ bra back
					cursor++;
				else
					break;
			}
			if(cursor == oldCursor)
				System.err.println("Failed to move cursor at src entry #" + (src - 1));
			for(int i = cursor - 1; i >= 0; i--)
			{
				a = (short) i;
				x = a;
				a >>= 3;
				usedCursor = a;
				a = x;
				a = (short) (a & 7);
				if((used[usedCursor] & (1 << a)) == 0)  
					System.err.println("Cursor moved past unmarked area at src entry #" + (src - 1));
			}
			if(cursor < outputSize && src == srcData.length)
			{
				viewUsedCursor(used, origBlock);
				System.out.println("Cursor now at " + cursor);
				System.out.println("-------------------");
				//viewC3Data(origBlock, width);
				compressBytes3(origBlock, 12, true);
				decompress3(0, srcData, width, outputSize, origBlock, true);
			}
			if(verbose)
			{
				viewUsedCursor(used, origBlock);
				System.out.println("Cursor now at " + cursor);
				System.out.println("-------------------");
			}
			x = popStack();
			if(cursor == outputSize)  //no need to emulate this in SNES
			{
				for(int i = 0; i < out.length; i++)
					dest.add(out[i]);
				dataSize = src - initSrc;
				return dest;
			}
		}
	}
	
	private void viewUsedCursor(byte[] used, byte[] orig)
	{
		int o = 0;
		int l = 0;
		int m = 12;
		for(int i = 0; i < used.length; i++)
		{
			for(int j = 0; j < 8; j++)
			{ 
				int x = used[i] & (1 << j);
				if(x == 0)
					System.out.print("0");
				else
					System.out.print("1");
				o++;
				if(o == 12)
				{
					if(m < 100)
						System.out.print("   " + m + "     ");
					else
						System.out.print("   " + m + "    ");
					for(int k = 0; k < 12; k++)
					{
						String s = Integer.toHexString(orig[k + l]);
						if(s.length() == 1)
							s = " " + s;
						System.out.print(s + ",");
					}
					System.out.println();
					o = 0;
					l += 12;
					m += 12;
				}
			}
		}
	}
	
	public void viewCompressionPairs()
	{
		System.out.print("DEC pairs:");
		for(int i = 0; i < decompPairs.size(); i += 2)
		{
			short a = decompPairs.get(i);
			short b = decompPairs.get(i + 1);
			System.out.print("(" + a + "," + b + ")");
		}
		System.out.println();
		System.out.print("COM pairs:");
		for(int i = 0; i < compPairs.size(); i += 2)
		{
			short a = compPairs.get(i);
			short b = compPairs.get(i + 1);
			System.out.print("(" + a + "," + b + ")");
		}
		System.out.println();
	}
	
	
	public ArrayList<Byte> compressBytes(byte[] in)
	{
		int c = 0;  //consumed
		//ArrayList<Byte> temp = new ArrayList<Byte>();
		//put d[0]
		ArrayList<Short> patternBytes = new ArrayList<Short>();
		ArrayList<Byte> controlBytes = new ArrayList<Byte>();
		ArrayList<Byte> copyBytes = new ArrayList<Byte>();
		compPairs = new ArrayList<Short>();
		controlBytes.add((byte) 1);
		copyBytes.add(in[c]);
		c++;
		while(c < in.length)
		{
			int ptnPos = 0;
			int ptnLen = 0;
			int bestPtnPos = 0;
			int bestPtnLen = 0;
			for(int i = c - 1; i >= 0; i--)
			{
				if(in[i] == in[c]) //look back finding matches
				{
					ptnPos = i;
					ptnLen = 0;
					while(in[ptnPos + ptnLen] == in[c + ptnLen])
					{
					   ptnLen++;
					   if(c + ptnLen >= in.length)
						   break;
					   if(ptnLen == 254)
						   break;
					}
					if(ptnLen > bestPtnLen)
					{
						bestPtnPos = c - ptnPos - 1;
						bestPtnLen = ptnLen;
					}
				}
				if(c - i > 4095)
					break;
			}
			if(bestPtnLen > 1)
			{
			    //put pattern
				controlBytes.add((byte) 0);
				patternBytes.add((short) bestPtnLen);
				patternBytes.add((short) bestPtnPos);
				compPairs.add((short) (bestPtnLen - 1));
				compPairs.add((short) bestPtnPos);
			    c += bestPtnLen;
			}
			else  //just dump the byte
			{
				controlBytes.add((byte) 1);
				copyBytes.add(in[c]);
				c++;
			}
		}	  
		patternBytes.add((short) 255);
		controlBytes.add((byte) 0);
		
		ArrayList<Byte> compPatterns = new ArrayList<Byte>();
		ArrayList<Byte> types = new ArrayList<Byte>();
		//compPatterns.add((byte) 0);
		//compPatterns.add((byte) 0);
		types.add((byte) 0);   
		types.add((byte) 0);
		types.add((byte) 2);
		//ArrayList<ArrayList<Byte>> rv = generatePatterns(patternBytes);
		ArrayList<Byte> out = new ArrayList<Byte>();
		//then weave them together
		byte control = 0;
		byte cshift = 7;
		int lastControl = 2;
		int nPatternBytes = 0;  //change to 0?
		int lastPattern = 0;
		width = 16;
		rem = 0;
		cursor = 0;
		int cleft = 0;
		writeByte = 0;
		//init - shift 2
		out.add((byte) 0);
		out.add((byte) 0);
		out.add(control);
		for(int i = 0; i < controlBytes.size(); i++)
		{
			//ArrayList<Byte> ptn = rv.get(i);
			byte ctrl = controlBytes.get(i);
			if(ctrl == 1)
			{
				control |= (1 << cshift);
				out.add(copyBytes.remove(0));
				types.add((byte) 1);
			}
			else
			{
				short len = patternBytes.get(lastPattern);
				short dist = 0;
				if(len != 255)
				{
					dist = patternBytes.get(lastPattern + 1);
					lastPattern += 2;
				}
				int dc = cursor;
				compPatterns = generatePatterns(compPatterns, len, dist);
				dc -= cursor;
				cleft += dc;
				while(cleft < 0)  //add 2 bytes of stream
				{
					types.add((byte) 0);
					types.add((byte) 0);
					out.add((byte) 0);
					out.add((byte) 0);
					cleft += 16;
				}
				/*for(int j = nPatternBytes; j < compPatterns.size(); j++)
				{
					types.add((byte) 0);
					out.add((byte) 0);
					//added++;
				}*/
				//nPatternBytes = compPatterns.size();
				/*if((added & 1) == 1)
				{
					types.add((byte) 0);
					out.add((byte) 0);
				}*/
			}
			cshift--;
			if(cshift < 0)
			{
				out.set(lastControl, control);
				control = 0;
				lastControl = out.size();
				out.add((byte) 0);
				types.add((byte) 2);
				cshift = 7;
			}
		}
		if(cshift == 7) //remove the last control byte (as it is unused)
		{
			for(int i = types.size() - 1; i >= 0; i--)
			{
				if(types.get(i) == 2)
				{
					types.remove(i);
					out.remove(i);
					break;
				}
			}
		}
		else
			out.set(lastControl, control);
		//finally insert the compressed pattern
		if((compPatterns.size() & 1) == 1)  //if patterns size is odd output a 0
		{
			compPatterns.add((byte) 0);
			types.add((byte) 0);
			out.add((byte) 0);
		}
		//System.out.print("stream:");
		//for(int i = 0; i < compPatterns.size(); i++)
			//System.out.print(UWNHRando.viewByte(compPatterns.get(i)) + ",");
		//System.out.println();
		/*int added = 0;
		for(int i = 0; i < types.size(); i++)   //ensure that stream bytes are delivered 2 at a time
		{
			if(types.get(i) == 0)
			{
				added++;
			}
			else
			{
				if((added & 1) == 1)
				{
					types.add(i, (byte) 0);
					out.add(i, (byte) 0);
				}
				added = 0;
			}
		}*/
		
		byte remove = 1;
		for(int i = 0; i < types.size(); i++)
		{
			if(types.get(i) == 0)
			{
				if(compPatterns.size() == 0)
				{
					//while(i < out.size())
					//out.remove(i);
					types.set(i,(byte) 3);  //it's a future byte
					//i--;
					continue;
					//break;
				}
				byte b = compPatterns.remove(remove);
				out.set(i, b);
				remove ^= 1;
			}
		}
		compMarking = types;
		return out;
	}
	
	int writeByte;
	ArrayList<Byte> queueValue(ArrayList<Byte> queue, short val, boolean useCursor)
	{
		//if(queue.size() == 0)
			//queue.add((byte) 0);
		
		val <<= (16 - width);
		int v1 = val << 16;
		if(rem == 0)
		{
			rem = 8;
			queue.add((byte) 0);  //add another byte
		}
		//check cursor with list size
		//int maxqueue = (int) (2 + Math.ceil(cursor / 8.0));
		//while(queue.size() > maxqueue)
			//queue.remove(queue.size() - 1);
		if(useCursor)
			writeByte = cursor / 8;
		while(writeByte >= queue.size())
			queue.add((byte) 0);
		while(true)
		{
			int sz = queue.size() - 1;
			byte w = queue.get(writeByte);
			
			//w <<= rem;
			int v2 = (v1 >> (32 - rem));
			int addMask = ((1 << rem) - 1);
			if(addMask > 255)
				addMask = 255;
			//System.out.println("\nwidth=" + width + " cursor=" + cursor + " write byte=" + writeByte + " rem=" + rem + " v2=" + v2 + " mask=" + addMask + " old=" + w);
			v2 &= addMask;
			w |= v2;
			//val <<= Math.min(8, rem);
			queue.set(writeByte, w);
			//width -= rem;
			if(width > rem)   //if you have more bits to encode
			{
				if(writeByte == sz)
					queue.add((byte) 0);  //add another byte
				rem += 8;
				writeByte++;
			}
			else
				break;
			
		}
		return queue;
	}
	
	byte width;
	byte rem;
	int cursor;
	ArrayList<Byte> generatePatterns(ArrayList<Byte> patternBytes, short length, short pos)  //this is the code that compresses
	{
		//ArrayList<ArrayList<Byte>> rv = new ArrayList<ArrayList<Byte>>();
		//byte width = 16;
        //int rem = 0;
        ArrayList<Byte> ptn = patternBytes;
        int adjLength = length;
        if(adjLength != 255)
        	adjLength--;
        //System.out.print("Putting length value " + adjLength + " at cursor position " + cursor);
		//while(patternBytes.size() > 0)
		//{
			
			//short length = patternBytes.remove(0);
			if(length == 255)
			{
				//System.out.println();
				/*rem++;
				if(rem > 8)
					rem -= 8;*/
				width = 14;
				ptn = queueValue(ptn, (short) 127, true);
				//rv.add(ptn);
				return ptn;
			}
			length--;
			int lenVal = 0;  //will be a combination of y value and length-1
			byte shift1 = 0;
			byte yout = -1;
			if(length == 1)
			{
				lenVal = 1;
				width = 1;
				queueValue(ptn, (short) 1, true);
				rem--;
				while(rem < 8)
					rem += 8;
				//cursor++;   //cursor moves 0 + 1
			}
			else 
			{
				if(length >= 128)  //y is 0
				{
					yout = 0;
					lenVal = (length - 128);
					//lenVal <<= 2;
					shift1 = 13;
				}
				else if(length >= 8)  //y is 1-8
				{
					//lenVal = length;
					//lenVal <<= 1;
					if(length >= 64)  //1
					{
						shift1 = 12;
						yout = 1;
						lenVal = (length - 64);
					}
					else if(length >= 32)  //2-3
					{
						yout = (byte) (length >> 4);
						lenVal = (length - (yout * 16));
						shift1 = 10;
					}
					else if(length >= 16)   //4-7
					{
						yout = (byte) (length >> 2);
						lenVal = (length - (yout * 4));
						shift1 = 8;
					}
					else
					{
						yout = (byte) length;
						lenVal = 0;
						shift1 = 6;
					}
				}
				else  //y is 8-64
				{
					/*yout = */
					lenVal = length;
					int[] distIndex = {0,0,32,48,16,20,24,28};
					yout = (byte) (distIndex[lenVal]);
					if(yout < 32)
						shift1 = 4;
					else
						shift1 = 2;
					lenVal = 0;
				}
				width = 7;
				//System.out.print(" yout=" + yout);
				ptn = queueValue(ptn, yout, true);
				rem -= width;
				while(rem <= 0)
				{
					rem += 8;
					writeByte++;
				}
				/*while(rem > 8)
				{
					rem -= 8;
					writeByte++;
				}*/
				if(shift1 > 7)
				{
					width = (byte) (shift1 - 6);
					//System.out.print(" remLen=" + lenVal);
					ptn = queueValue(ptn, (short) lenVal, false);
				}
			}
			//output the maximum of yout and len
			//short out = (short) Math.max(lenVal, yout);
			//shift the cursor forward shift1 + 1 bits
			
			cursor += shift1 + 1;
			rem = (byte) (8 - (cursor % 8));
			//while(rem < 0)
				//rem += 8;
			byte aa = 0;
			byte bb = 0;
			int p1 = 0;
			int p2 = 0;
			if(ptn.size() >= 2)
			{
				p2 = ptn.size() - 1;
				p1 = ptn.size() - 2;
				aa = ptn.get(p1);
				bb = ptn.get(p2);
			}
			else if(ptn.size() == 1)
			{
				p1 = ptn.size() - 1;
				aa = ptn.get(p1);
			}
			//System.out.println("  [" + p1 + ":" + UWNHRando.viewByte(aa) + " " + p2 + ":" + UWNHRando.viewByte(bb) + "]");
			//while(rem > 8)
				//rem -= 8;
			//System.out.print("Putting dist value " + pos + " at cursor position " + cursor);
			//out <<= shift1;
			//now pattern distance (pos)
			int patOut = 0;
			byte shift2 = 0;
			//short pos = patternBytes.remove(0);
			if(pos < 128)  //0-127; >= 128 breaks the array lookup
			{
				//y is patOut
				if(pos < 4)
					patOut = pos * 8;
				else if (pos < 8)
					patOut = 32 + ((pos - 4) * 4);
				else if(pos < 32)
					patOut = (pos * 2) + 32;
				else
					patOut = pos + 64;
				shift2 = (byte) (patOut >> 4);
				byte[] sLookup = {7,7,8,9,9,9,10,10,10,10,10,10};
				shift2 = sLookup[shift2];
				width = 9;
				ptn = queueValue(ptn, (short) patOut, true);
				cursor += (shift2 - 1);
				
			}
			else  
			{
				patOut  = pos;
				if(pos < 256)
				{
					y = 3;
					shift2 = 11;
				}
				else if(pos < 512)
				{
					y = 4;
					shift2 = 12;
				}
				else if(pos < 1024)
				{
					y = 5;
					shift2 = 13;
				}
				else if(pos < 2048)
				{
					y = 6;
					shift2 = 14;
				}
				else if(pos < 4096)
				{
					y = 7;
					shift2 = 15;
				}
				//patOut |= (y << 13);
				//put y in a width of 3
				width = 3;
				ptn = queueValue(ptn, (short) y, true);
				rem -= width;
				while(rem <= 0)
				{
					rem += 8;
					writeByte++;
				}
				width = (byte) (shift2 - 4);
			//	System.out.print(" remPat=" + pos);
				ptn = queueValue(ptn, (short) pos, false);
				//put pos in a width of shift2 - 3
				//advance cursor shift2
				cursor += shift2 - 1;
			}
			if(ptn.size() >= 2)
			{
				p2 = ptn.size() - 1;
				p1 = ptn.size() - 2;
				aa = ptn.get(p1);
				bb = ptn.get(p2);
			}
			else if(ptn.size() == 1)
			{
				p1 = ptn.size() - 1;
				aa = ptn.get(p1);
			}
			//System.out.println("  [" + p1 + ":" + UWNHRando.viewByte(aa) + " " + p2 + ":" + UWNHRando.viewByte(bb) + "]");
			rem = (byte) (8 - (cursor % 8));
			//while(rem < 0)
				//rem += 8;
			//out = (short) patOut;
			
			/*ptn = queueValue(ptn, out);
			width = shift2;
			rem -= width;
			while(rem < 0)
				rem += 8;
			while(rem > 8)
				rem -= 8;*/
			
			//rv.add(ptn);
		//}
		return ptn;
	}
	
	public int getMapSize(ArrayList<ArrayList<Byte>> map)
	{
		int rv = 0;
		for(int i = 1; i < map.size(); i+=2)
			rv += map.get(i).size();
		return rv;
	}
	
	private boolean testBlock(byte[] block1, byte[] block2)
	{
		for(int i = 0; i < 4; i++)
			if(block1[i] != block2[i])
				return false;
		return true;
	}
	
	class IndexedTileBlock implements Indexed
	{
		byte[] tileBlock;
		int count;
		int tileIndex;
		int newTileIndex;
		boolean adjusted;
		byte[] adjTileBlock;
		
		IndexedTileBlock(byte[] blk, int c, int ti)
		{
			tileBlock = blk;
			count = c;
			tileIndex = ti;
		}
		
		public void setNewTileIndex(int nti, boolean changed, byte[] chgTo)
		{
			newTileIndex = nti;
			adjusted = changed;
			adjTileBlock = chgTo;
		}
		
		public long getIndex()
		{
			return count;
		}
		
		public boolean isSame(byte[] o)
		{
			return testBlock(tileBlock, o);
		}

		public void printInfo(int listIndex) 
		{
			String s = "#" + listIndex + ": count=" + count + " idx=" + tileIndex + " data=" + Arrays.toString(tileBlock);
			System.out.println(s);
		}
	}
	
	private int scoreSubDiff(byte a, byte b)
	{
		if((a & b) == -128)
		{
			if(a != b)
				return 1;
		}
		//ports and discoveries >= 189 - do not touch these
		if((a >= -67 && a < 0) || (b >= -67 && b < 0))
			return 100;
		if((a >= 180 && a <= 187) || a == 134)  //desert stuff
		{
			if((b >= 180 && b <= 187) || b == 134)
				return 100;
			else
				return 1;
		}
		if((b >= 180 && b <= 187) || b == 134)  //desert stuff
		{
			if((a >= 180 && a <= 187) || a == 134)
				return 100;
			else
				return 1;
		}
		
		byte[][] smap = new byte[5][];
		//byte[] land = {128, 168, 134, 144, 171, 150, 175, 156, 178, 162};
		//byte[] v1 = {(byte) 128,(byte) 128,(byte) 128};  //arctic
		//smap[0] = v1;
		byte[] v2 = {(byte) 168, (byte) 144};  //tundra
		smap[1] = v2;
		byte[] v3 = {(byte) 171, (byte) 150};  //temperate
		smap[2] = v3;
		byte[] v4 = {(byte) 175, (byte) 156};  //subtropical
		smap[3] = v4;
		byte[] v5 = {(byte) 178, (byte) 162};  //tropical
		smap[4] = v5;
		int ai = 0; 
		int bi = 0;
		for(int i = 1; i < 5; i++)
		{
			for(int j = 0; j < smap[i].length; j++)
			{
				if(a == smap[i][j])
					ai = i;
				if(b == smap[i][j])
					bi = i;
			}
		}
		//land style diff
		if(ai != bi)
			return 100;
		else
			return 1;
		//cliff diff
		/*if((a & 15) != (b & 15))
			return 10;
		return 20;*/
	}
	
	private int scoreTile(byte tile, byte[] box)  //we want low scores
	{
		int diff = 0;
		int tloc = 983552 - 512 + 64 + 4 * tile;
		for(int i = 0; i < 4; i++)
			if(rom.data[tloc + i] != box[i])
				diff += scoreSubDiff(rom.data[tloc + i], box[i]);
		return diff;
	}
	
	private byte nearestTileIndex(byte[] box)
	{
		int tiles = 983552 - 512;
		
		ArrayList<Byte>[] matches = new ArrayList[4];
		for(int i = 0; i < 4; i++)
			matches[i] = new ArrayList<Byte>();
		
		for(int i = 0; i < 960; i += 4)
		{
			int m = 0;
			for(int j = 0; j < 4; j++)
			{
				if(rom.data[tiles + 64 + (i) + j] == box[j])
					m++;
			}
			if(m == 4)
				return ((byte) (i >> 2));    //a total match; this should not happen
			matches[m].add((byte) (i >> 2));
		}
		int lowScore = Integer.MAX_VALUE;
		byte lowTile = 0;
		for(int i = 3; i >= 0; i--)
		{
			for(int j = 0; j < matches[i].size(); j++)
			{
				byte ct = matches[i].get(j);  //candidate tile
				int score = scoreTile(ct, box);
				if(score < lowScore)
				{
					lowScore = score;
					lowTile = ct;
				}
			}
		}
		
		if(lowScore >= 100)
			return 0;
		//if(lowScore >= 4)
			//System.out.println("possible map error");
		return lowTile;
	}
	
	private ArrayList<Byte> processBlocks(ArrayList[] bhash, ArrayList<Integer> blocks)  //builds the list of tile blocks according to their counts (calculated previously)
	{
		
		ArrayList<Byte> rv = new ArrayList<Byte>(blocks.size());
		//int[] equiv = new int[blocks.size()];
		/*for(int i = 0; i < blocks.size(); i++)
			rv.add((byte) 0);*/
		
		//calculate a count of used blocks  - no need anymore
		int tiles = 983552 - 512;
		/*int[] counts = new int[256 + blocks.size()];
		int orig = 0;
		
		for(int k = 0; k < blocks.size(); k++)
		{
			if(counts[k + 256] == -1)
				continue;
			int fval = -1;
			byte[] box = blocks.get(k);
			for(int i = 0; i < 1024; i += 4)
			{
				if(rom.data[tiles + i] == box[0])
				{
					for(int j = 1; j < 4; j++)
					{
						if(rom.data[tiles + i + j] != box[j])
							break;
						if(j == 3)
							fval = (i / 4);
					}
				}
				if(fval > -1)
				{
					counts[fval]++;
					//rv.set(k, (short) fval);
					orig++;
					break;
				}
			}
			if(fval > -1)
			{
				//blocks.remove(k);
				//k--;
				//rv.set(k, (byte) fval);
				equiv[k] = fval;
				counts[k] = -1;
				continue;
			}
			if(counts[k + 256] == -1)
				continue;
			counts[k + 256]++;
			equiv[k] = k + 256;
			for(int i = k + 1; i < blocks.size(); i++)
			{
				//if(blocks.get(k)[0] == -112)
					//System.out.print("");
				if(testBlock(blocks.get(k), blocks.get(i)))
				{
					counts[k + 256]++;
					
					//rv.set(i, k);
					equiv[i] = k + 256;
					counts[i + 256] = -1;
					//blocks.remove(i);
					//i--;
				}
			}
		}*/
		//determine the top 240
		IndexedList il = new IndexedList();
		il.allowDuplicates(true);
		/*for(int i = 0; i < 256; i++)
		{
			if(counts[i] > 0)
			{
				IndexedTileBlock itb = new IndexedTileBlock(Arrays.copyOfRange(rom.data, i * 4, i * 4 + 4), counts[i], i);
				il.add(itb);
			}
		}*/
		for(int i = 0; i < bhash.length; i++)
		{
			for(int j = 0; j < bhash[i].size(); j++)
			{
				il.add((IndexedTileBlock) bhash[i].get(j));
				/*if(counts[256 + i] > 0)
				{
					IndexedTileBlock itb = new IndexedTileBlock(blocks.get(i), counts[i + 256], i + 256);
					il.add(itb);
				}*/
			}
		}
		/*for(int i = 0; i < il.size(); i++)
		{
			IndexedTileBlock itb = (IndexedTileBlock) il.get(i);
			System.out.println(itb.count + " (#" + itb.tileIndex + ") " + Arrays.toString(itb.tileBlock));
		}*/
		//put in the top 240
		int low240 = 99999;
		for(int i = 0; i < 240; i++)
		{
			int idx = il.size() - i - 1;
			if(idx < 0)
				break;
			IndexedTileBlock itb = (IndexedTileBlock) il.get(idx);
			if(itb.count < low240)
				low240 = itb.count;
			for(int j = 0; j < 4; j++)
				rom.data[tiles + 64 + (4 * i) + j] = itb.tileBlock[j];
		}
		//write 4x0 to tile
		for(int i = 0; i < 4; i++)
			rom.data[tiles + i] = 0;
		/*for(int i = 0; i < 240; i++)  //test loop
		{
			int idx = il.size() - i - 1;
			if(idx < 0)
				break;
			IndexedTileBlock itb = (IndexedTileBlock) il.get(idx);
			for(int j = 0; j < 4; j++)
				if(rom.data[tiles + 64 + (4 * i) + j] != itb.tileBlock[j])
					System.out.println("tile placement error at tile #" + i + " entry #" + j);
		}*/
		for(int i = 0; i < il.size(); i++)
		{
			IndexedTileBlock itb = (IndexedTileBlock) il.get(i);
			//itb.printInfo(i);
		}
		ArrayList<Integer> adjIndeces = new ArrayList<Integer>();
		for(int i = 0; i < il.size(); i++)
		{
			int idx = il.size() - i - 1;
			IndexedTileBlock itb = (IndexedTileBlock) il.get(idx);
			//itb.setNewTileIndex(i, false, null);
			if(i < 240)
			{
				itb.setNewTileIndex(i + 16, false, null);
			}
			else	
				adjIndeces.add(itb.tileIndex);
		}
		int replacedCount = 0;
		int adjustedCount = 0;
		for(int k = 0; k < blocks.size(); k++)
		{
			//boolean found = false;
			int equiv = blocks.get(k);
			for(int i = 0; i < il.size(); i++)
			{
				int idx = il.size() - i - 1;
				IndexedTileBlock itb = (IndexedTileBlock) il.get(idx);
				if(equiv == itb.tileIndex)
				{
					if(i < 240)
					{
						rv.add((byte) itb.newTileIndex);
						replacedCount++;
					}
					else
					{
						byte[] blk = itb.tileBlock;
						byte adjIndex = nearestTileIndex(blk);
						rv.add(adjIndex);
						if(adjIndex == 0)
						{
							byte[] newBlock = {0,0,0,0};
							itb.setNewTileIndex(adjIndex, true, newBlock);
						}
						for(int r = il.size() - 1; r >= 0; r--)
						{
							IndexedTileBlock itb2 = (IndexedTileBlock) il.get(r);
							if(itb2.tileIndex == adjIndex)
							{
								itb.setNewTileIndex(adjIndex, true, itb2.tileBlock);
								break;
							}
						}
						System.out.println("Changed #" + itb.tileIndex + Arrays.toString(itb.tileBlock) + "  to #" + itb.newTileIndex + Arrays.toString(itb.adjTileBlock));
						if(adjIndeces.contains(equiv) == false)
							System.err.println("Error: replacing tileset " + equiv + " that should not be replaced - adjInd failure");
						if(itb.count > low240)
							System.err.println("Error: replacing tileset " + equiv + " that should not be replaced - low240 failure");
						adjustedCount++;
					}
					
					//found = true;
					break;
				}
				//if(testBlock(blocks.get(k), itb.tileBlock))
			}
			/*if(!found)
			{
				for(int i = 240; i < il.size(); i++)
				{
					int idx = il
				}
				
			}*/
		}
		for(int i = 0; i < il.size(); i++)
		{
			IndexedTileBlock itb = (IndexedTileBlock) il.get(i);
			int ni = itb.newTileIndex;
			//System.out.print("tb#" + i + " ni=" + ni + " adj=" + itb.adjusted);
			byte[] compareTo = null;
			if(itb.adjusted)
				compareTo = itb.adjTileBlock;
			else
				compareTo = itb.tileBlock;
			//System.out.print("  arr=" + Arrays.toString(itb.tileBlock));
			//if(itb.adjTileBlock != null)
				//System.out.print("  adjTB=" + Arrays.toString(itb.adjTileBlock));
			//System.out.println();
			int start = tiles + (4 * ni);
			byte[] tarr = Arrays.copyOfRange(rom.data, start, start  + 4);
			for(int j = 0; j < 4; j++)
			{
				if(tarr[j] != compareTo[j])
					System.out.println("Tile mismatch at tile #" + ni + " adj=" + itb.adjusted + " exp=" + Arrays.toString(compareTo) + " actual=" + Arrays.toString(tarr));
			}
		}
		//System.out.println("Original count=" + orig);
		System.out.println("Replaced count=" + replacedCount);
		System.out.println("Adjusted count=" + adjustedCount);
		/*ArrayList<Byte> rv2 = new ArrayList<Byte>(rv.size());
		for(int i = 0; i < rv.size(); i++)
			rv2.add((byte) (rv.get(i) & 255));
		return rv2;*/
		return rv;
	}
	
	public ArrayList<ArrayList<Byte>> compressMap(GameMap map)
	{
		map.backup();
		//map.listCounts();
		//map.cliffify();  //no need to do this (should make for smaller maps)
		//map.listCounts();
		map.zonify();
		//map.listCounts();
		map.desertify();
		//map.listCounts();
		ArrayList<ArrayList<Byte>> out = new ArrayList<ArrayList<Byte>>();
		int[] deltas = new int[45 * 90];
		int totalSize = 0;
		ArrayList<Integer> allBlocks = new ArrayList<Integer>();
		ArrayList<byte[]> chunks = new ArrayList<byte[]>();
		ArrayList<IndexedTileBlock>[] allTileBlocks = new ArrayList[256];
		for(int i = 0; i < 256; i++)
			allTileBlocks[i] = new ArrayList<IndexedTileBlock>();
		int blkIndex = 0;
		int blockCount = 0;
		for(int yy = 0; yy < 45; yy++)
		{
			for(int xx = 0; xx < 90; xx++)
			{
				byte[] chunk = new byte[144];
				int cd = 0;
				for(int y = 0; y < 24; y += 2)
				{
					for(int x = 0; x < 24; x += 2)
					{
						int xo = xx * 24 + x;
						int yo = yy * 24 + y;
						byte[] box = new byte[4];
						box[0] = map.fullMap[yo][xo];
						box[2] = map.fullMap[yo + 1][xo];
						box[1] = map.fullMap[yo][xo + 1];
						box[3] = map.fullMap[yo + 1][xo + 1];
						//if(yo + 1 == 1079)
							//System.out.println();
						//if(box[0] == -127)
							//System.out.println();
						
						int special = 0;
						for(int i = 0; i < 4; i++)
							special |= box[i] & 127;
						if(special == 0)
						{
							//if the box contains nothing special output a simple 0-15 box
							int fval = 0;
							for(int i = 0; i < 4; i++)
							{
								fval <<= 1;
								if(box[i] != 0)
									fval |= 1;
							}
							chunk[cd] = (byte) fval;
							cd++;		
						}
						else
						{
							int bx = box[0] & 255;
							if(bx >= 129 && bx < 134)
								System.out.println("Curious value at " + xo + "," + yo);
							ArrayList<IndexedTileBlock> lst = allTileBlocks[bx];
							boolean found = false;
							for(int i = 0; i < lst.size(); i++)
							{
								IndexedTileBlock blk = lst.get(i);
								if(blk.isSame(box))
								{
									found = true;
									blk.count++;
									allBlocks.add(blk.tileIndex);
									break;
								}
							}
							if(!found)
							{
								lst.add(new IndexedTileBlock(box, 1, blkIndex));
								allBlocks.add(blkIndex);
								blkIndex++;
							}
							//allBlocks.add(box);
							blockCount++;
							chunk[cd] = (byte) 255;
							cd++;
						}
							//else lookup the appropriate value
						/*	
							if(fval == -1)
								System.out.println("Couldn't find sequence " + Arrays.toString(box));
							chunk[cd] = (byte) fval;
							cd++;
						}*/
					}
				}
				chunks.add(chunk);
			}
		}
		ArrayList<Byte> repl = processBlocks(allTileBlocks, allBlocks);
		int currChunk = 0;
		ArrayList<Integer> allDeltas = new ArrayList<Integer>();
		for(int yy = 0; yy < 45; yy++)
		{
			for(int xx = 0; xx < 90; xx++)
			{
				byte[] chunk = chunks.get(currChunk);
				int deltab = currChunk;
				currChunk++;
				for(int i = 0; i < chunk.length; i++)
				{
					if(chunk[i] == -1)
						chunk[i] = repl.remove(0);
				}
				
				//System.out.println("====================================");
				//System.out.print("oc=");
				//System.out.println(Arrays.toString(chunk));
				ArrayList<Byte> btsSrc = new ArrayList<Byte>();
				for(int i = 0; i < chunk.length; i++)
					btsSrc.add(chunk[i]);
				ArrayList<Byte> bts = compressBytes(chunk);
				
				
				//The following is test code
				byte[] c2 = new byte[bts.size()];
				for(int i = 0; i < bts.size(); i++)
					c2[i] = bts.get(i);
				ArrayList<Byte> test = decompress(0, c2);
				c2 = new byte[test.size()];
				for(int i = 0; i < test.size(); i++)
					c2[i] = test.get(i);
				for(int i = 0; i < c2.length; i++)
					if(c2[i] != chunk[i])
						System.err.println("re-decompressed chunk for [" + xx + "," + yy + "] was different at #" + i + "; expected " + chunk[i] + " actual " +  c2[i]);
				boolean match = true;
				int[] errors = {0,0,0,0};
				byte[][] aa = new byte[24][24];
				byte[][] bb = new byte[24][24];
				for(int y = 0; y < 24; y += 2)
				{
					for(int x = 0; x < 24; x += 2)
					{
						int iny = y / 2;
						int inx = x / 2;
						byte b = chunk[iny * 12 + inx];
						int xo = xx * 24 + x;
						int yo = yy * 24 + y;
						byte[] box = new byte[4];
						box[0] = map.fullMap[yo][xo];
						box[2] = map.fullMap[yo + 1][xo];
						box[1] = map.fullMap[yo][xo + 1];
						box[3] = map.fullMap[yo + 1][xo + 1];
						aa[y][x] = box[0];
						aa[y+1][x] = box[2];
						aa[y][x+1] = box[1];
						aa[y+1][x+1] = box[3];
						byte[] box2 = new byte[4];
						int special = 0;
						for(int i = 0; i < 4; i++)
							special |= box[i] & 127;
						if(special == 0)
						{
							//if the box contains nothing special output a simple 0-15 box
							int fval = 0;
							for(int i = 0; i < 4; i++)
							{
								fval <<= 1;
								if(box[i] != 0)
									fval |= 1;
							}
							if(b != fval)
								errors[0]++;
								//System.err.println("Chunk mismatch for [" + xx + "," + yy + "] in special: expected=" + fval + "  actual=" + b);
							if((fval & 1) > 0)
								box2[3] = -128;
							if((fval & 2) > 0)
								box2[2] = -128;
							if((fval & 4) > 0)
								box2[1] = -128;
							if((fval & 8) > 0)
								box2[0] = -128;
							bb[y][x] = box2[0];
							bb[y+1][x] = box2[2];
							bb[y][x+1] = box2[1];
							bb[y+1][x+1] = box2[3];
							for(int i = 0; i < 4; i++)
								if(box[i] != box2[i])
									//System.err.println("Chunk box mismatch for [" + xx + "," + yy + "] in special: expected=" + box[i] + " actual=" + box2[i]);
									errors[1]++;
							
						}
						if(special > 0)
						{
							int errType = 2;
							int bx = box[0] & 255;
							ArrayList<IndexedTileBlock> lst = allTileBlocks[bx];
							boolean found = false;
							for(int i = 0; i < lst.size(); i++)
							{
								IndexedTileBlock blk = lst.get(i);
								if(blk.isSame(box))
								{
									if(blk.adjusted == true)
									{
										box = blk.adjTileBlock;
										errType = 3;
									}
									//System.out.println("blk=" + Arrays.toString(blk.tileBlock) + "  orig=" + Arrays.toString(box) + "  b=" + b + " blk.nti=" + ((byte) blk.newTileIndex));
									found = true;
									break;
								}
							}
							if(!found)
							{
								//System.out.println("Block " + Arrays.toString(box) + " not found in list #" + bx);
								for(int i = 0; i < allTileBlocks.length; i++)
								{
									ArrayList<IndexedTileBlock> lst2 = allTileBlocks[bx];
									found = false;
									for(int j = 0; j < lst2.size(); j++)
									{
										IndexedTileBlock blk = lst2.get(j);
										if(blk.isSame(box))
										{
											if(blk.adjusted == true)
											{
												box = blk.adjTileBlock;
												errType = 3;
											}
											//System.out.println("Found block in list #" + i);
											//System.out.println("blk=" + Arrays.toString(blk.tileBlock) + "  orig=" + Arrays.toString(box) + "  b=" + b + " blk.nti=" + ((byte) blk.newTileIndex));
											found = true;
											break;
										}
									}
								}
								if(!found)
									System.out.println("Block was not found anywhere");
							}
							int tiles = 983552 - 512;
							for(int i = 0; i < 4; i++)
							{
								int bval = b & 255;
								byte tb = rom.data[tiles + (4 * bval) + i];
								box2[i] = tb;
								
								if(tb != box[i])
								{
									//System.err.println("Chunk mismatch for [" + xx + "," + yy + "] in tile: expected=" + box[i] + " actual=" + bb);
									System.out.println("above is mismatch");
									errors[errType]++;
								}
							}
							bb[y][x] = box2[0];
							bb[y+1][x] = box2[2];
							bb[y][x+1] = box2[1];
							bb[y+1][x+1] = box2[3];
						}
					}
				}
				for(int i = 0; i < errors.length; i++)
					if(errors[i] > 0)
						match = false;
				if(!match)
				{
					System.err.println("Chunk mismatch for [" + xx + "," + yy + "] errors=" + Arrays.toString(errors));
					viewChunkData(chunk);
					System.err.println("Expanded data comparison:");
					viewExpandedData(aa, bb);
				}
				//end test code
				//System.out.println("====================================");
				//test to see if it is a unique chunk
				int foundi = -1;
				for(int i = 1; i < out.size(); i += 2)
				{
					boolean found = true;
					ArrayList<Byte> bo = out.get(i);
					if(bo.size() != bts.size())
						continue;
					for(int j = 0; j < bts.size(); j++)
					{
						if(bo.get(j) != bts.get(j))
						{
							found = false;
							break;
						}
					}
					if(found)
					{
						foundi = i;
						break;
					}
				}
				if(foundi < 0)
				{
					out.add(btsSrc);
					out.add(bts);
					deltas[deltab] = totalSize;
					allDeltas.add(totalSize);
					totalSize += bts.size();
				}
				else
					deltas[deltab] = allDeltas.get(foundi / 2);
			}
		}
		System.out.println("done testing compression chunks");
		System.out.println("Repl left=" + repl.size());
		//collate the map
		//ArrayList<Byte> rv = new ArrayList<Byte>();
		//for(int i = 0; i < out.size(); i++)
			//rv.addAll(out.get(i));
		compDeltas = deltas;
		map.compressedSize = totalSize;
		System.out.println(totalSize + " bytes used out of 77313 bytes available");
		//for(int i = 0; i < chunks.size(); i++)
			//System.out.println(Arrays.toString(chunks.get(i)));
		deltaIndeces = getDeltaIndeces();
		return out;
	}

	private void viewExpandedData(byte[][] aa, byte[][] bb) 
	{
		for(int y = 0; y < aa.length; y++)
		{
			for(int x = 0; x < aa[0].length; x++)
			{
				String s = Integer.toHexString(aa[y][x] & 255);
				if(s.length() == 1)
					s = " " + s;
				System.err.print(s + " ");
			}
			System.err.print("      ");
			for(int x = 0; x < bb[0].length; x++)
			{
				String s = Integer.toHexString(bb[y][x] & 255);
				if(s.length() == 1)
					s = " " + s;
				System.err.print(s + " ");
			}
			System.err.println();
		}
		
	}


	private void viewChunkData(byte[] chunk) 
	{
		for(int y = 0; y < 12; y++)
		{
			for(int x = 0; x < 12; x++)
			{
				int idx = 12 * y + x;
				String s = Integer.toHexString(chunk[idx] & 255);
				if(s.length() == 1)
					s = " " + s;
				System.err.print(s + " ");
			}
			System.err.println();
		}
	}


	public int[][] getDeltas()
	{
		int[][] rv = new int[45][];
		for(int i = 0; i < 45; i++)
			rv[i] = new int[90];
		for(int y = 0; y < 45; y++)
		{
			for(int x = 0; x < 90; x++)
			{
				int d = compDeltas[y * 90 + x];
				/*int i = 0;
				while(d > 0)
				{
					d -= bts.get(i).size();
					i++;
				}*/
				rv[y][x] = d;
			}
		}
		return rv;
	}
	
	int[][] deltaIndeces;
	public int[][] getDeltaIndeces()
	{
		if(deltaIndeces != null)
			return deltaIndeces;
		int[][] rv = new int[45][];
		for(int i = 0; i < 45; i++)
			rv[i] = new int[90];
		ArrayList<Integer> allDeltas = new ArrayList<Integer>();
		for(int y = 0; y < 45; y++)
		{
			for(int x = 0; x < 90; x++)
			{
				int d = compDeltas[y * 90 + x];
				int ii = allDeltas.indexOf(d);
				if(ii == -1)
				{
					allDeltas.add(d);
					ii = allDeltas.size() - 1;
				}
				rv[y][x] = ii;
			}
		}
		deltaIndeces = rv;
		return rv;
	}
	
	//int[] overflowDeltas;
	public void collateMap(ArrayList<ArrayList<Byte>> bts, int[] freeAreas, GameMap map) 
	{
		int blk = 1;
		//byte[][] rv = new byte[freeAreas.length][]; 
		//overflowDeltas = new int[freeAreas.length];
		//ArrayList<Integer> needsUpdate = new ArrayList<Integer>();
		//int startD = Integer.MAX_VALUE;
		map.chunks = new GameMap.CompressionChunk[bts.size() / 2];
		for(int loc = 0; loc < freeAreas.length; loc++)
		{
			int used = 0;
			//overflowDeltas[loc] = (blk - 1) / 2;
			ArrayList<Byte> mp = new ArrayList<Byte>();
			for(int i = blk; i < bts.size(); i += 2)
			{
				used += bts.get(i).size();
				if(used > freeAreas[loc])
					break;
				mp.addAll(bts.get(i));
				byte[] chunk = new byte[bts.get(i).size()];
				for(int j = 0; j < chunk.length; j++)
					chunk[j] = bts.get(i).get(j);
				map.chunks[i / 2] = map.new CompressionChunk(chunk, i / 2, 0, loc);
				blk = i;
			}
			//collate the list of grabbed blocks into the byte list stored at the given available area
			/*byte[] out = new byte[mp.size()];
			for(int i = 0; i < out.length; i++)
				out[i] = mp.get(i);
			rv[loc] = out;*/
			if(blk == bts.size())
				break;
			//at this point we gather indeces that need updating
			/*int end = (blk - 1) / 2;
			if(loc == 0) //collect the initial list
			{
				for(int yy = 0; yy < deltaIndeces.length; yy++)
				{
					for(int xx = 0; xx < deltaIndeces[yy].length; xx++)
					{
						int ii = deltaIndeces[yy][xx];
						if(ii > end)
						{
							needsUpdate.add(yy * deltaIndeces[yy].length + xx);
							if(ii < startD)
								startD = ii;
						}
					}
				}
			}
			else
			{
				for(int i = 0; i < needsUpdate.size(); i++)
				{
					int ii = needsUpdate.get(i);
					if(ii > end)
						continue;
					//compDeltas[ii] -= startD;
					//compDeltas[ii] += availLocs[loc];
					needsUpdate.remove(i);
					i--;
				}
				startD = Integer.MAX_VALUE;
				for(int i = 0; i < needsUpdate.size(); i++)
				{
					int ii = needsUpdate.get(i);
					if(ii < startD)
						startD = ii;
				}
			}*/
		}
		//return rv;
	}

	public byte[][][] collectBlocks(ArrayList<ArrayList<Byte>> bts, int[][] deltaIndeces) 
	{
		byte[][][] rv = new byte[45][90][];
		//int l = 0;
		for(int yy = 0; yy < 45; yy++)
		{
			for(int xx = 0; xx < 90; xx++)
			{
				int delta = deltaIndeces[yy][xx];
				ArrayList<Byte> orig = bts.get(delta * 2);
				int sz = orig.size();
				rv[yy][xx] = new byte[sz];
				for(int i = 0; i < sz; i++)
					rv[yy][xx][i] = orig.get(i);
				//l += 2;
			}
		}
		return rv;
	}
	
}

class PortSelPanel extends JPanel
{
	String portName;
	int basePortType;
	int newPortType;
	public Rectangle nextRect;
	public Rectangle prevRect;
	public Rectangle[] convertRect;
	
	PortSelPanel()
	{
		prevRect = new Rectangle(0,0,150,100);
		nextRect = new Rectangle(450,0,150,100);
		setPreferredSize(new Dimension(600,100));
		portName = "";
		convertRect = new Rectangle[9];
		for(int i = 0; i < 9; i++)
		{
			convertRect[i] = new Rectangle(153 + 30 * i, 50, 27, 50);
		}
		//addMouseListener(this);
	}
	
	public void setPName(String name)
	{
		portName = name;
		repaint();
	}
	
	public void setBasePortType(int val)
	{
		basePortType = val;
	}
	
	public void setNewPortType(int val)
	{
		newPortType = val;
		repaint();
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 600, 100);
		g.setColor(Color.MAGENTA);
		g.fillRect(0,0,150,100);
		g.fillRect(450, 0, 150, 100);
		
		for(int i = 0; i < convertRect.length; i++)
		{
			if(newPortType == i)
				g.setColor(Color.GREEN);
			else if(basePortType == i)
				g.setColor(Color.ORANGE);
			else
				g.setColor(Color.MAGENTA);
			Rectangle r = convertRect[i];
			g.fillRect(r.x, r.y, r.width, r.height);
		}
		
		g.setColor(Color.YELLOW);
		g.drawString("Prev", 10, 60);
		g.drawString("Next", 460, 60);
		g.drawString(portName, 180, 40);
		for(int i = 0; i < convertRect.length; i++)
			g.drawString(String.valueOf(i), 156 + 30 * i, 90);
	}
}

class PortPanel extends JPanel
{
	Port port;
	byte[][] tileTypes;
	byte[][] lastTileTypes;
	short[][] hiTypes;
	
	public static Color[] portColor = {
			new Color(0, 200, 200),  //European Shingle
			new Color(0, 150, 150),  //European Wood
			new Color(150, 150, 0),  //Thatched Roof (Caribbean)
			new Color(50, 50, 0),    //Hut (African)
			new Color(200,200,200),  //Middle Eastern
			new Color(100, 100, 0),  //Hut (Spice Islands)
			new Color(200, 200, 0),  //Indian
			new Color(200, 50, 50),  //Chinese
			new Color(200, 50, 150)  //Japanese
	};
	
	public static final byte WATER = 64;
	public static final byte SAND = 72;
	public static final byte PARK = 80;
	public static final byte ROAD = 88;
	
	PortPanel(Port p)
	{
		if(p != null)
			setPort(p, p.tileType, true);
	}
	
	private void backupOld()
	{
		lastTileTypes = new byte[tileTypes.length][];
		for(int i = 0; i < tileTypes.length; i++)
		{
			lastTileTypes[i] = Arrays.copyOf(tileTypes[i], tileTypes[i].length);
		}
	}
	
	public void setPort(Port p, int portTileType, boolean testIt)
	{
		if(p == null)
			return;
		if(port != null  && portTileType != port.tileType)
			backupOld();
		else if(port != null && p.index != port.index)
			backupOld();
		else
			lastTileTypes = null;
		port = p;
		tileTypes = new byte[32][32];
		hiTypes = new short[32][32];
		for(int yy = 0; yy < 32; yy++)
		{
			for(int xx = 0; xx < 32; xx++)
			{
				if(port.layout[yy][xx] == 75)
					tileTypes[yy][xx] = WATER;
				else if(port.layout[yy][xx] == 22)
					tileTypes[yy][xx] = PARK;
				else if(port.layout[yy][xx] == 28)
					tileTypes[yy][xx] = ROAD;
				else
					tileTypes[yy][xx] = (byte) port.getTileType(port.layout[yy][xx], portTileType);
				/*if(lastTileTypes != null)
				{
					//port.testPort(portTileType);
					//if(tileTypes[yy][xx] != lastTileTypes[yy][xx])
						//System.out.println("Warning: difference in tile type at " + xx + "," + yy + " old=" + lastTileTypes[yy][xx] + " new=" + tileTypes[yy][xx]);
				}*/
			}
		}
		if(lastTileTypes != null && testIt)
		{
			port.testPort(portTileType);
		}
		updateHiTypes();
		repaint();
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.black);
		g.fillRect(0,  0,  100,  100);
		Color base = portColor[port.tileType];
		int rr = base.getRed();
		int gg = base.getGreen();
		int bb = base.getBlue();
		for(int yy = 0; yy < 32; yy++)
		{
			for(int xx = 0; xx < 32; xx++)
			{
				int val = tileTypes[yy][xx];
				if(val >= 0 && val < 45)
					g.setColor(new Color(rr + val, gg + val, bb + val));
				else if(val == WATER)
					g.setColor(Color.BLUE);
				else if(val == SAND)
					g.setColor(Color.YELLOW);
				else if(val == PARK)
					g.setColor(Color.GREEN);
				else if(val == ROAD)
					g.setColor(Color.LIGHT_GRAY);
				else
					g.setColor(Color.GRAY);
				g.fillRect(xx * 20, yy * 20, 20, 20);
			}
		}
		g.setFont(new Font(Font.SANS_SERIF, 0, 10));
		if(rr + gg + bb < 250)
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.BLACK);
		for(int yy = 0; yy < 32; yy++)
		{
			for(int xx = 0; xx < 32; xx++)
			{
				int val = port.layout[yy][xx];
				String s = Integer.toHexString(val & 255);
				g.drawString(s, xx * 20 + 3, yy * 20 + 10);
				s = Integer.toHexString(hiTypes[yy][xx]);
				g.drawString(s, xx * 20 + 3, yy * 20 + 20);
			}
		}
	}

	public void updateHiTypes() 
	{
		for(int yy = 0; yy < 32; yy++)
			for(int xx = 0; xx < 32; xx++)
				hiTypes[yy][xx] = port.toHigh(port.layout[yy][xx]);
	}
}

class PortWindow extends JFrame implements MouseListener
{
	PortPanel pan;
	PortSelPanel span;
	int currPort;
	SNESRom rom;
	
	PortWindow(SNESRom rom)
	{
		this.rom = rom;
		currPort = 0;
		pan = new PortPanel(null);
		span = new PortSelPanel();
		Port pp = initPort(currPort);
		getContentPane().add(span, BorderLayout.NORTH);
		getContentPane().add(pan, BorderLayout.CENTER);
		currPort = 0;
		addMouseListener(this);
		setSize(660,660);
		setVisible(true);
	}
	
	private Port initPort(int idx)
	{
		ArrayList<Integer> portData = UWNHRando.getPortData(rom);
		int pStart = portData.get(idx);
		//start = all.get(k);
		Compressor comp = new Compressor(rom);
		ArrayList<Byte> ba = comp.gameDecompress(pStart); 
		byte tileset = rom.data[735038 - 512 + idx];
		Port p = new Port(idx);
		p.tileType = tileset;
		p.origTileType = tileset;
		p.load(ba);
		p.setName(UWNHRando.getPortName(idx, rom));
		p.backup();
		span.setPName(currPort + ". " + p.name);
		span.setBasePortType(p.tileType);
		span.setNewPortType(p.tileType);
		pan.setPort(p, p.tileType, false);
		return p;
	}
	
	private void switchPortTileType(int idx)
	{
		System.out.println("Switching type to " + idx);
		span.setNewPortType(idx);
		pan.port.restoreBackup();
		pan.port.convertTileType(idx, this);
		pan.setPort(pan.port, idx, true);
		//pan.updateHiTypes();
		//pan.repaint();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		
		int mx = e.getX();
		int my = e.getY() - 26;
		//System.out.println("MP x" + mx + "  Y" + my + "  pr=" + span.prevRect.contains(mx, my) + " nr=" + span.nextRect.contains(mx, my));
		
		if(span.prevRect.contains(mx, my))
			advancePort(-1);
		if(span.nextRect.contains(mx, my))
			advancePort(1);
		for(int i = 0; i < span.convertRect.length; i++)
		{
			Rectangle r = span.convertRect[i];
			if(r.contains(mx, my))
			{
				switchPortTileType(i);
				return;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
	}
	
	private void advancePort(int amt)
	{
		currPort += amt;
		if(currPort < 0)
			currPort = 99;
		if(currPort > 99)
			currPort = 0;
		initPort(currPort);
		
	}
}

class Port
{
	int x;
	int y;
	int index;
	String name;
	byte shopType;
	byte syType;
	byte origTileType;
	byte tileType;
	boolean isCapital;
	int buildingsFailed;
	
	byte[][] layout;
	byte[][] backupLayout;
	
	boolean[][] buildingSelection;
	boolean[][] replaced;
	
	private static ArrayList<Short>[][] replacePattern;
	private static ArrayList<Short>[][] findPattern;
	private static boolean[] allowsWide;
	private static boolean[] hasHighFloor;
	private static ArrayList<String> allDirs;
	private static int[] bldgLefts;
	private static int[] bldgRights;
	private static int[] bldgTops;
	private static int[] bldgBots;
	private static int[] shadows;
	private static int[] nonShadows;
	
	public static int[][] sizeDiffs;
	
	ArrayList<Point> portDoors;
	private boolean goingBack;
	
	ArrayList<Byte> compressedBytes;
	
	static byte GRASS = 15;
	
	class BuildingPan extends JPanel
	{
		Rectangle brect;
		
		BuildingPan(Rectangle r)
		{
			brect = r;
			//paintImmediately(0,0,500,500);
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.black);
			g.fillRect(0,  0,  500, 500);
			Color base = PortPanel.portColor[tileType];
			int rr = base.getRed();
			int gg = base.getGreen();
			int bb = base.getBlue();
			for(int yy = brect.y; yy < brect.y + brect.height; yy++)
			{
				for(int xx = brect.x; xx < brect.x + brect.width; xx++)
				{
					int val = layout[yy][xx];
					if(val >= 80 || val < 0)
						g.setColor(new Color(rr, gg, bb));
					else if(val == 75)
						g.setColor(Color.BLUE);
					//else if(val == SAND)
						//g.setColor(Color.YELLOW);
					else if(val == 22)
						g.setColor(Color.GREEN);
					else if(val == 28)
						g.setColor(Color.LIGHT_GRAY);
					else
						g.setColor(Color.GRAY);
					g.fillRect((xx - brect.x) * 20, (yy - brect.y) * 20, 20, 20);
				}
			}
			g.setFont(new Font(Font.SANS_SERIF, 0, 10));
			if(rr + gg + bb < 250)
				g.setColor(Color.WHITE);
			else
				g.setColor(Color.BLACK);
			for(int yy = brect.y; yy < brect.y + brect.height; yy++)
			{
				for(int xx = brect.x; xx < brect.x + brect.width; xx++)
				{
					byte val = layout[yy][xx];
					String s = Integer.toHexString(val & 255);
					g.drawString(s, (xx - brect.x) * 20 + 3, (yy - brect.y) * 20 + 10);
					s = Integer.toHexString(toHigh(layout[yy][xx]));
					g.drawString(s, (xx - brect.x) * 20 + 3, (yy - brect.y) * 20 + 20);
				}
			}
		}
		
	}
	
	class BuildingView extends JFrame
	{
		BuildingPan bp;
		BuildingView(Rectangle r, boolean befAft)
		{
			bp = new BuildingPan(r);
			getContentPane().add(bp);
			setLocation(500, 0);
			setSize(300, 300);
			initTitle(r, befAft);
			setVisible(true);
			bp.paintImmediately(0,0,500,500);
		}
		
		private void initTitle(Rectangle r, boolean befAft)
		{
			String s = "Bldg at [" + r.x + "," + r.y + ",w=" + r.width + ",h=" + r.height + "]:";
			if(befAft)
				s += " After";
			else
				s += " Before";
			setTitle(s);
		}
	}
	
	public Port(int idx)
	{
		index = idx;
		layout = new byte[32][32];
		isCapital = false;
		//Lisbon, Seville, Istanbul, Genoa, London, Amsterdam, also Mecca requires the Palace setup
		int[] capitals = {0,1,2,8,29,33,77};
		for(int i = 0; i < capitals.length; i++)
		{
			if(idx == capitals[i])
			{
				isCapital = true;
				break;
			}
		}
		if(findPattern == null)
			generateReplacePatterns();
		if(allowsWide == null)
		{
			boolean[] wideB = {true, true, true, false, true, false, true, false, false};
			boolean[] hiFloor = {false, false, false, true, false, true, false, false, false};
			allowsWide = wideB;
			hasHighFloor = hiFloor;
			int[] shadowsI =    {17, 18, 20, 21, 23, 24, 26, 27, 29, 30};
			shadows = shadowsI;
			int[] shadowBaseI = {16, 16, 19, 19, 22, 22, 25, 25, 28, 28};
			nonShadows = shadowBaseI;
		}
		portDoors = new ArrayList<Point>();
	}
	
	public void load(ArrayList<Byte> in)
	{
		for(int y = 31; y >= 0; y--)
			for(int x = 31; x >= 0; x--)
				layout[y][x] = in.remove(in.size() - 1);
		fixPortErrors();   //only do this if reading from the original port
	}
	
	public void backup()
	{
		backupLayout = new byte[layout.length][];
		for(int i = 0; i < backupLayout.length; i++)
		{
			backupLayout[i] = Arrays.copyOf(layout[i], layout[i].length);
		}
	}
	
	public ArrayList<Byte> getConvertedPort()
	{
		ArrayList<Byte> rv = new ArrayList<Byte>();
		for(int yy = 0; yy < 32; yy++)
			for(int xx = 0; xx < 32; xx++)
				rv.add(layout[yy][xx]);
		return rv;
	}
	
	public void restoreBackup()
	{
		layout = new byte[backupLayout.length][];
		for(int i = 0; i < layout.length; i++)
		{
			layout[i] = Arrays.copyOf(backupLayout[i], backupLayout[i].length);
		}
	}
	
	private void viewPortData(int x, int y, int x2, int y2)
	{
		int x0 = x;
		while(y <= y2)
		{
			x = x0;
			while(x <= x2)
			{
				int tt = getTileType(layout[y][x]);
				System.out.print(tt + ",");
				x++;
			}
			y++;
			System.out.println();
		}
	}
	
	private void fixPortConversionProblems()
	{
		switch(index)
		{
			//not necessarily errors, but we need to replace the bottom right corner of buildings with patios
			//so that selectBldg() does not select patio tiles (or ones underneath) [d86->d6e] [->d66] 
			case 91:  //Sunda  @28,19
				layout[19][28] = toLow(Short.parseShort("d6e", 16));
				layout[19][29] = toLow(Short.parseShort("d66", 16));
				break;
			case 89:  //Dili @27,18
				layout[18][27] = toLow(Short.parseShort("d6e", 16));
				layout[18][28] = toLow(Short.parseShort("d66", 16));
				break;
			case 88:  //Banda  @14,7
				layout[7][14] = toLow(Short.parseShort("d6e", 16));
				layout[7][15] = toLow(Short.parseShort("d66", 16));
				break;
			case 86:  //Malacca  @15,19
				layout[19][15] = toLow(Short.parseShort("d6e", 16));
				layout[19][16] = toLow(Short.parseShort("d66", 16));
				break;
			case 84:  //Amboa  @11,20
				layout[20][11] = toLow(Short.parseShort("d6e", 16));
				layout[20][12] = toLow(Short.parseShort("d66", 16));
				break;
			//African huts - long buildings (8 across) must be split up into two
			case 65:  //Abidjan - long building at @7,16 must be split up
				layout[16][10] = toLow(Short.parseShort("d44", 16));
				layout[16][11] = toLow(Short.parseShort("d40", 16));
				layout[17][10] = toLow(Short.parseShort("d4a", 16));  //"d4a";
				layout[17][11] = toLow(Short.parseShort("d46", 16));  //"d46";
				layout[18][10] = toLow(Short.parseShort("d64", 16));  //"d64";
				layout[18][11] = toLow(Short.parseShort("d4c", 16));  //"d4c";
				layout[19][11] = toLow(Short.parseShort("d66", 16));  //"d66";
				break;
			case 60:  //Bissau - long building at @12,15 must be split up
				layout[15][15] = toLow(Short.parseShort("d44", 16));
				layout[15][16] = toLow(Short.parseShort("d40", 16));
				layout[16][15] = toLow(Short.parseShort("d4a", 16));  //"d4a";
				layout[16][16] = toLow(Short.parseShort("d46", 16));  //"d46";
				layout[17][15] = toLow(Short.parseShort("d64", 16));  //"d64";
				layout[17][16] = toLow(Short.parseShort("d4c", 16));  //"d4c";
				layout[18][16] = toLow(Short.parseShort("d66", 16));  //"d66";
				break;
			case 57:  //Madeira - long building must be split on roof 15,7, 15,8, 16,7, 16,8
				layout[7][15] = toLow(Short.parseShort("d44", 16));  //"d44";
				layout[7][16] = toLow(Short.parseShort("d40", 16));  //"d40";
				layout[8][15] = toLow(Short.parseShort("d4a", 16));  //"d4a";
				layout[8][16] = toLow(Short.parseShort("d46", 16));  //"d46";
				layout[9][15] = toLow(Short.parseShort("d64", 16));  //"d64";
				layout[9][16] = toLow(Short.parseShort("d4c", 16));  //"d4c";
				layout[10][16] = toLow(Short.parseShort("d66", 16));  //"d66";
				//viewPortData(12, 7, 21, 11);
				break;
		}
	}
	
	private void fixPortErrors()
	{
		switch(index)
		{
		case 94:  //Zeiton
			layout[13][25] = toLow(Short.parseShort("d60", 16));
			layout[13][27] = toLow(Short.parseShort("d64", 16));
			break;
		case 66:  //Sofala
			layout[18][13] = toLow(Short.parseShort("d64", 16));
			break;
		case 58:  //Santa Cruz
			layout[22][20] = toLow(Short.parseShort("d4e", 16));
			layout[22][22] = toLow(Short.parseShort("d62", 16));
			break;
		case 57: //Madeira
			layout[7][5] = toLow(Short.parseShort("d4c", 16));
			layout[7][9] = toLow(Short.parseShort("d64", 16));
			layout[9][12] = toLow(Short.parseShort("d4c", 16));
			layout[9][19] = toLow(Short.parseShort("d64", 16));
			layout[9][22] = toLow(Short.parseShort("d4e", 16));
			layout[9][24] = toLow(Short.parseShort("d62", 16));
			layout[15][15] = toLow(Short.parseShort("d4e", 16));
			layout[15][17] = toLow(Short.parseShort("d62", 16));
			layout[18][20] = toLow(Short.parseShort("d4c", 16));
			layout[18][21] = toLow(Short.parseShort("d4e", 16));
			layout[18][24] = toLow(Short.parseShort("d64", 16));
			layout[19][23] = toLow(Short.parseShort("d6c", 16));
			layout[19][24] = toLow(Short.parseShort("d6e", 16));
			layout[21][13] = toLow(Short.parseShort("d4e", 16));
			layout[21][15] = toLow(Short.parseShort("d62", 16));
			layout[24][7] = toLow(Short.parseShort("d4e", 16));
			layout[24][9] = toLow(Short.parseShort("d62", 16));
			break;
		case 16:  //Athens
			layout[3][9] = toLow(Short.parseShort("d46", 16));
			break;
		case 7:  //Marsielle
			layout[10][8] = toLow(Short.parseShort("dae", 16));
			break;
		}
	}
	
	private void generateReplacePatterns() 
	{
		replacePattern = new ArrayList[9][];
		findPattern = new ArrayList[9][];
		allDirs = new ArrayList<String>();
		int[] lft = {6,8,9,10,17};
		int[] rgt = {5,7,11,12,18};
		int[] top = {9,11,14,20,21};
		int[] bot = {2,4,7,8};
		bldgLefts = lft;
		bldgRights = rgt;
		bldgTops = top;
		bldgBots = bot;
		
		//input all the data - this is really long
		String[][] all = {{"e26","e26","e22","e22","e24","e22","e22","e22","e22"},   //door
				{"d8c","d8c","da2;d4c;first","d4e;d62;wh5","d64","d4e;d62;wh5","d80;d8c;s/t","dc8;d6e;d8e;dae;d82;d84;d86;d88;d8a;357patcen","da0;d48;d4a;d4c;d4a;d4c;d68;d6a;d6c;d6a;d6c;357patran"},  //front wall
				{"dac","dac","d6c;d6e;first","d68;e0e;d6c;wh5","d4e","d68;e0e;d6c;d86;d8c;wh5","da0","d4c;d6a;da2;da4;da8;daa;357patcen","d8c;d82;d86;357patran"},  //wall bottom
				{"d4a","d4a","d8c","sw","d66","sw","d82","sw","sw"},  //shadow wall
				{"d6a","d6a","d8e","bsw","d86","bsw","da2","bsw","bsw"},  //bottom shadow wall
				{"d8e","d8e","da4","d64;wh5","daa","d64;wh5","d8e","dc0;d8c;dc4;chkw","d4e;d6e;chkw"},  //wall edge R
				{"dc0;d86;first","dc0;d86;first","da0","d4c;wh5","da8","d4c;wh5","d8a","d4e;d80;dc2;chkw","d46;d66;chkw"},  //wall edge L
				{"dae","dae","daa","d6e","dca","d6e","dae","d6c;dac;dac;first","d88;d8e;first"},  //wall bottom right
				{"da6","da6","da6","d66","dc8","d66;d84;first","daa","d4a;da0;da0;first","d80;d8a;first"},  //wall bottom left
				{"d4c;hut","d4c;hut","d48;hut","d40","dc2;hut","d40","d40;hut","d40;hut","d40;hut"},  //roof P corner  - 9 to 21 is roof
				{"d6c;da0;first","d6c;da0;first","d68;d86;first","d46","d88","d46","d60;da4;Irf","d60","d60"},  //roof L corner
				{"d4e","d4e","d4a","d44","dc4","d44","d4c","d48","d44"},  //roof 7 corner
				{"d6e;da4;first","d6e;da4;first","d6a;d8a;first","d4a","d8a","d4a","d6c;da8;Irf","d68","d64"},  //roof J corner
				{"d68;da2;first","d68;da2;first","d66;d88;first","d48","d4c;d46;first","d48","da6;d62;d6a;Irf","d62;d64;d66;f3mpat","d62"},  //roof lower edge 13
				{"d48;d40;d44;first","d48;d40;d44;first","d46;d40;d44;first","d42","d44","d42","d46;d48;d4a;Irf","d42;d44;d46;f3mpat","d42"},  //roof upper edge 14
				{"fwr","fwr","fwr","trb","d48","trb","Irf","trb","trb"},  //roof inner 7 corner  15
				{"fwr","fwr","fwr","trb","d4a","trb","Irf","trb","trb"},  //roof inner P corner  16
				{"d60;d80;fwr","d60;d80;fwr","d60;d80;fwr","trb","d68;ich","trb","d84","trb","trb"},  //roof left edge  17
				{"d64;d84;fwr","d64;d84;fwr","d64;d84;fwr","trb","d6a;ich","trb","d88;dc2;first","trb","trb"},  //roof right edge 18
				{"d62;d82;fwr","d62;d82;fwr","d62;d82;fwr","trb","da4","trb","d86","trb","trb"},  //roof center 19
				{"d46;d66;t/b","d46;d66;t/b","nr","nr","nr","nr","d40;d42;d44;d46;d48;d4a;d4c;d60;d62;d64;d66;d68;d6a;d6c;dc0;Irf","nr","nr"},  //roof feature - 20
				{"d42;d60;d62;d64;d80;d82;d84;da0;da2;da4;fwr","d42;d60;d62;d64;d80;d82;d84;da0;da2;da4;fwr","d42;d60;d62;d64;d80;d82;d84;d86;d88;d8a;fwr","trb","nr","trb","Irf","trb","trb"},  //forward roof - 21
				{"w/g","w/g","w/g","w/g","da6;dc6;d40;d60;d80;da0;dc0;d42;d62;d82;da2;d6c;d8c;dac;dcc","w/g","w/g","w/g","w/g"},  //middle eastern sceptre feature - 22
				{"gr","gr","dac;j1","d80;j1","e00;j1","da0;j1","dc4;j1","dca;j1","da2;j1"},  //sign stand
				{"grb","grb","grb","grb","grb","d8a;d88","grb","grb","grb"},  //patio
				{"gr","gr","gr","gr","gr","d8e","d6e","gr","gr"},   //pottery
				{"e06","e06","","","d6e","","","",""},  //palace brick 1  - palace replacements start at 26
				{"dc4","dc4","","","d8e","","","",""},  //palace brick 2
				{"de4","de4","","","dae","","","",""},  //palace brick 3
				{"dc2","dc2","","","e04","","","",""},  //palace turret 1
				{"dc6","dc6","","","e08","","","",""},  //palace turret 2
				{"dc8","dc8","","","e0c","","","",""},  //palace turret 3
				{"de2","de2","","","e06","","","",""},   //palace turret 4
				{"de6","de6","","","e0a","","","",""},   //palace turret 5
				{"dca","dca","","","de0","","","",""},  //palace door top 1
				{"dcc","dcc","","","de2","","","",""},  //palace door top 2
				{"dce","dce","","","de4","","","",""},  //palace door top 3
				{"dea","dea","","","de6","","","",""},  //palace door mid 1
				{"dec","dec","","","de8","","","",""},  //palace door mid 2
				{"dee","dee","","","dea","","","",""},  //palace door mid 3
				{"e00","e00","","","dec","","","",""},  //palace door bot 1
				{"e24","e24","","","e22","","","",""},  //palace door bot 2
				{"e04","e04","","","dce","","","",""},  //palace door bot 3
				{"de0","de0","","","e02","","","",""},   //palace window
				{"gr", "gr", "dae;dc0;dc2;dc4", "gr", "gr", "gr", "gr", "gr", "gr"},  //cactus and totems in Caribbean ports (->grass) (44)
				{"di", "di", "di", "di", "e0e;e20", "di", "di", "di", "di", "di"}  //(45)
		};
		for(int cts = 0; cts < findPattern.length; cts++)  //cts = cultural tile set
		{
			findPattern[cts] = new ArrayList[all.length];
			replacePattern[cts] = new ArrayList[all.length];
			for(int t = 0; t < all.length; t++)
			{
				String s = all[t][cts];
				String[] cell = s.split(";");
				ArrayList<String> directives = new ArrayList<String>();
				replacePattern[cts][t] = new ArrayList<Short>();
				findPattern[cts][t] = new ArrayList<Short>();
				for(int i = 0; i < cell.length; i++)
				{
					processCell(findPattern[cts][t], replacePattern[cts][t], directives, cell[i]);
				}
				ArrayList<Short> dmap = new ArrayList<Short>(directives.size());
				for(int i = 0; i < directives.size(); i++)
				{
					dmap.add(mapDirective(directives.get(i)));
				}
				if(dmap.size() > 0)
				{
					replacePattern[cts][t].add((short) 0);
					replacePattern[cts][t].addAll(dmap);
					dmap.clear();
				}
			}
		}
	}
	
	private void processCell(ArrayList<Short> vals, ArrayList<Short> vals2, ArrayList<String> dirs, String cell)
	{
		if(cell.length() == 0)
			return;
		try
		{
			short val = Short.parseShort(cell, 16);
			vals.add(val);
			vals2.add(val);
		}
		catch(NumberFormatException ex)
		{
			String dir = cell;
			dirs.add(dir);
		}
	}
	
	private short mapDirective(String in)
	{
		int idx = allDirs.indexOf(in);
		if(idx >= 0)
			return (short) idx;
		allDirs.add(in);
		return (short) (allDirs.size() - 1);
	}

	public void setXY(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void setName(String newName)
	{
		name = newName;
		if(name.length() > 16)
			name = name.substring(0, 15);
	}
	
	public void setMarketType(byte in)
	{
		shopType = in;
		if(in == -1)
			shopType = (byte) (UWNHRando.rand() * 13);
	}
	
	public void setShipyardType(byte in)
	{
		syType = in;
		if(in == -1)
			syType = (byte) (UWNHRando.rand() * 9);
	}
	
	public void decompressPort(SNESRom rom)
	{
		int start = 1738904;  //1a8898
		int deltas = 650959 - 512; //9eecf
		
		deltas += 3 * index;
		int delta = 0;
		for(int i = 0; i < 3; i++)
		{
			int d = (int) (rom.data[deltas + i] & 255);
			delta += (d << (8 * i));
		}
		
		Compressor comp = new Compressor(rom);
		ArrayList<Byte> portData = comp.gameDecompress(start + delta);
		int ii = 0;
		for(int y = 0; y < 32; y++)
		{
			for(int x = 0; x < 32; x++)
			{
				layout[y][x] = portData.get(ii);
				ii++;
			}
		}
	}
	
	public short toHigh(byte in)  //converts the byte value of a port to the actual tile index
	{
		byte vlow = (byte) (in & 7);
		short vmid = (short) (in & 255);
		short vhigh = (short) (vmid >> 3);
		vhigh <<= 5;
		vhigh += (vlow << 1);
		return (short) (vhigh + 3072);
	}
	
	public byte toLow(short in)  //converts the tile index to the stored byte value
	{
		byte vlow = (byte) ((in & 15) >> 1);
		short vhigh = (short) (in & 32752);  //7ff0
		vhigh -= 3072;
		vhigh >>= 2;
		vhigh += vlow;
		return (byte) vhigh;
	}
	
	private boolean isSignOrPost(int x, int y)
	{
		if(layout[y][x] >= 0 && layout[y][x] < 16)
			return true;
		int tt = getTileType(layout[y][x]);
		if(tt == 23)
			return true;
		return false;
	}
	
	public Point convertBuilding(Rectangle building, int newType, PortWindow portWindow)
	{
		for(int y = building.y; y < building.y + building.height; y++)
		{
			for(int x = building.x; x < building.x + building.width; x++)
			{
				Point p = convertSingleTile(x, y, newType, -1, building);
				if(portWindow != null)
				{
					portWindow.pan.setPort(this, newType, false);
					portWindow.pan.paintImmediately(0,0,800,800);
				}
				if(p != null)
				{
					x = p.x;
					y = p.y;
					if(y < building.y || x < building.x)  
					{
						if(goingBack)
						{
							p.x = building.x + building.width;
							p.y = building.y + building.height - 1;
							return p;
						}
						else
							return p;
					}
					if(x >= building.x + building.width || y >= building.y + building.height)
					{
						if(goingBack)
						{
							p.y = building.y;
							p.x = building.x - 1;
							return p;
						}
						else 
							return p;
					}
					//System.out.println("Resetting cursor to point within the building");
				}
			}
		}
		return null;
	}
	
	public Point convertSingleTile(int x, int y, int newType, int replList, Rectangle brect)
	{
		//System.out.println("Converting " + x + "," + y);
		if(replaced[y][x])
			return null;
		int found = replList;  //allows direct replacement
		boolean replacementPreset = true;
		if(found == -1)
		{
			replacementPreset = false;
			found = getTileType(layout[y][x]);
			if(found == -1)
				return null;
		}
		ArrayList<Short> repl = replacePattern[newType][found];
		if(repl.size() == 1)
		{
			byte nval = toLow(repl.get(0));
			layout[y][x] = nval;
			//System.out.println("Normal processing point " + x + "," + y);
		}
		else  //use directive
		{
			short val = toHigh(layout[y][x]);
			int d0 = repl.indexOf((short) 0) + 1;
			short dd = repl.get(d0);
			String adr = allDirs.get(dd);
			//System.out.println("   using directive " + adr);
			if(adr.equals("hut"))  //hut check
			{
				//System.out.println("Hut check on " + x + "," + y);
				Point p = hutCheck(x, y, newType, brect);
				if(p == null)
				{
					byte nval = toLow(repl.get(0));
					layout[y][x] = nval;
				}
				else
				{
					//System.out.println("Hut check moved building");
					return p;
				}
			}
			else if(adr.equals("wh5"))  //enforce a building of width 5
			{
				int origX = x;
				int origY = y;
				int[] p = bldgWidthMax5(x, y, newType);
				if(p[0] == -1)  
				{
					//System.out.println("Did not convert non-building at " + x + "," + y + " tt=" + found);
					//bldgWidthMax5(x, y, newType);
					return null;
				}
				if(p[0] != x)
					x = p[0];
				/*if(p[2] == -1)  //you split a building; need to go back
				{
					y = p[1];
					x = p[0];
					return new Point(x, y);
				}*/
				x = p[0];
				y = p[1];
				int w = p[2];
				int replBase = 0;
				int st = 0;
				int end = 3;
				if(w == 5)
				{
					st++;
					end++;
				}
				//boolean[][] bldg = selectBldg(x, y);
				//at this point we have not processed anything
				boolean[][] bldg = selectRoof(x, y - 1, newType, false);
				if(bldg == null)
				{
					System.out.println("Failed to find building at " + x + "," + y + " after calling maxBldgWidth5 at " + origX + "," + origY);
					bldg = selectRoof(x + 1, y - 1, newType, false);
				}
				Rectangle rrect = getSelectedRectangle(bldg);
				rrect.height++;  //grow the height 1 because the building has 2 rows of roof and 1 row of wall
				//System.out.println("Walling up building at " + rrect.toString());
				brect = rrect;
				Point[] doors = getDoors(brect, false);
				//System.out.println("Door is at " + doors[0].toString());
				//place inner wall using wall tiles 
				//always done on African ports and only 5 wide Spice Island ports
				if(newType == 3 || w == 5)
				{
					for(int i = st; i < end; i++)
					{
						//int vv = toHigh();
						int tt = getTileType(layout[y][x + i]);
						if(tt > 0 && tt < 9)  //tt will be -1 on sign; 0 on door
						//if(findPattern[tileType][1].contains(vv) ||
						//   findPattern[tileType][2].contains(vv))
						{
							//System.out.println("Replacing " + (x + i) + "," + y + " with wall; tt=" + tt);
							short v = findPattern[newType][1].get(replBase);
							layout[y][x + i] = toLow(v);
							replBase++;
							replaced[y][x + i] = true;
						}
						else if(tt != 0)
							replBase++;
						//else
							//System.out.println("Skipping " + (x + i) + "," + y + " with tt=" + tt);
					}
				}
				//x--;
				//place outer wall using lr end tiles
				//always done on Spice Island ports and only 5 wide African ports
				if(newType == 5 || w == 5)
				{
					//left side
					layout[y][x] = backupLayout[y][x];
					if(!isSignOrPost(x, y))
					{
						//System.out.println("Replacing " + x + "," + y + " with L Wall");
						layout[y][x] = toLow(findPattern[newType][6].get(0));
						replaced[y][x] = true;
					}
					//right side
					if(!isSignOrPost(x + w - 1, y))
					{
						//System.out.println("Replacing " + (x + w - 1) + "," + y + " with R Wall");
						layout[y][x + w - 1] = toLow(findPattern[newType][5].get(0));
						replaced[y][x + w - 1] = true;
					}
				}
				//finally place the floor
				/*boolean[][] bldg = selectBldg(x, y);
				Rectangle brect = getSelectedRectangle(bldg);
				Point[] doors = getDoors(brect, false);*/
				//System.out.println("finishing building at " + brect.toString() + "  door=" + doors[0].toString());
				if(bldgCanGrowDown(brect))
				{
					for(int xx = 0; xx <= w - 1; xx++)
					{
						//if(isSignOrPost(x + xx, y + 1))
							//continue;
						if(layout[y +1][x + xx] >= 0 && layout[y + 1][x + xx] < 16)  //sign
							continue;
						/*if(findPattern[tileType][23].contains(toHigh(layout[y + 1][x + xx]))) //sign stand
							continue;*/
						if(xx == 0)
						{
							if(w == 3 && newType == 3)
								layout[y + 1][x] = toLow(findPattern[newType][2].get(0));
							else
								layout[y + 1][x] = toLow(findPattern[newType][8].get(0));
						}
						else if(xx == w - 1)
						{
							if(w == 3 && newType == 3)
								layout[y + 1][x + w - 1] = toLow(findPattern[newType][2].get(2));
							else
								layout[y + 1][x + w - 1] = toLow(findPattern[newType][7].get(0));
						}
						else if(x + xx < doors[0].x)
							layout[y + 1][x + xx] = toLow(findPattern[newType][2].get(0));
						else if(x + xx == doors[0].x)
							layout[y + 1][x + xx] = toLow(findPattern[newType][2].get(1));
						else		
							layout[y + 1][x + xx] = toLow(findPattern[newType][2].get(2));
						
						replaced[y + 1][x + xx] = true;
						//System.out.println("Putting bottom floor at " + (x + xx) + ", " + (y + 1));
					}
					int f = Arrays.binarySearch(shadows, layout[y + 1][x + w]);
					if(f < 0)
						f = Arrays.binarySearch(nonShadows, layout[y + 1][x + w]);
					if(f >= 0)
						layout[y + 1][x + w] = (byte) (nonShadows[f] + 1);
					f = Arrays.binarySearch(shadows, layout[y][x + w]);
					if(f < 0)
						f = Arrays.binarySearch(nonShadows, layout[y][x + w]);
					if(f >= 0)
						layout[y][x + w] = (byte) (nonShadows[f] + 2);
				}
				else
				{
					//System.out.println("Building at " + brect.toString() + " failed to place floor");	
				}
				//lock the roof in place (necessary because you moved the roof down)
				for(int i = 1; i < 3; i++)
				{
					for(int xx = 0; xx < w; xx++)
						replaced[y - i][x + xx] = true;
				}
				
			}
			else if(adr.equals("s/t"))  //2nd if door is below, 1st otherwise
			{
				short l = toHigh(backupLayout[y + 1][x]);
				if(l == findPattern[tileType][0].get(0))
					layout[y][x] = toLow(repl.get(1));
				else
					layout[y][x] = toLow(repl.get(0));
			}
			else if(adr.startsWith("357pat"))  //3-5-7 pattern; replace a whole building facade
			{
				int[] p = null;
				//System.out.println("Processing 357pat on point " + x + "," + y);
				boolean rand = false;
				if(adr.endsWith("cen"))
					p = bldgWidth357(x, y, newType, true, brect);
				else
				{
					p = bldgWidth357(x, y, newType, false, brect);
					rand = true;
				}
				if(p[0] != x)
					x = p[0];
				/*if(p[2] == -1)  //you split a building; need to go back
				{
					y = p[1];
					x--;
					System.out.println("Split building at " + x + "," + y);
					return new Point(x, y);
				}*/
				//note: found is 2 when you found a lower wall
				int w = p[2];
				int replBase = 0;
				if(w == 5)
					replBase = 1;
				else if(w == 7)
					replBase = 4;
				boolean jCheckerStart = false;
				if(found == 2)
				{
					replBase = 0;
					if(w == 7 && !rand)
						replBase = 2;
				}
				else if(w > 3 && rand)
				{
					if(UWNHRando.rand() < 0.5)
						replBase = 1;
					else
					{
						replBase = 6;
						jCheckerStart = true;
					}
				}
				//System.out.println("Reverting to point " + x + "," + y + "; w=" + w);
				int doorx = -1;
				int doory = y;
				for(int i = 1; i < w - 1; i++)
				{
					int tt = getTileType(layout[y][x + i]);
					if(tt > 0 && !isSignOrPost(x + i, y))
					{
						short v = findPattern[newType][found].get(replBase);
						layout[y][x + i] = toLow(v);
						if(found == 1 || !rand)
							replBase++;
						//System.out.println((x + i) + "," + y + " marked as replaced with wall");
						replaced[y][x + i] = true;
					}
					else if(tt == 0)
						doorx = x + i;
					else
					{
						//System.out.println("Did not convert " + (x + i) + "," + y + "; tt was " + tt + " for " + layout[y][x + i]);
						//isSignOrPost(x + i, y);
					}
				}
				if(found == 1)
				{
					doory++;
					replBase = 0;
					if(w == 7 && !rand)
					{
						replBase = 2;
						//p[3] sets the replBase as well
						if(p[3] == 2)  //the first door of a multi-door building is 2 spots away from the start
							replBase = 3;
					}
					for(int i = 1; i < w - 1; i++)
					{
						int tt = getTileType(layout[y + 1][x + i]);
						if(tt > 0 && !isSignOrPost(x + i, y + 1))  //wall bottom
						{
							short v = findPattern[newType][2].get(replBase);
							layout[y + 1][x + i] = toLow(v);
							if(!rand)
							{
								replBase++;
								if(replBase == 6)
									replBase = 2;
							}
							//System.out.println((x + i) + "," + (y + 1) + " marked as replaced with wall bottom");
							replaced[y + 1][x + i] = true;
						}
						else if(tt == 0)
							doorx = x + i;
						/*else
						{
							System.out.println("Did not convert " + (x + i) + "," + (y + 1) + "; tt was " + tt + " for " + layout[y + 1][x + i]);
							isSignOrPost(x + i, y + 1);
						}*/
					}
				}
				//Japanese random door l/r
				if(rand)
				{
					if(doorx - x >= 2 && x + w - doorx > 2)
					{
						if(UWNHRando.rand() > 0.5)
						{
							if(!isSignOrPost(doorx - 1, doory))
								layout[doory][doorx - 1] = toLow(findPattern[newType][2].get(1));
							if(!isSignOrPost(doorx + 1, doory))
								layout[doory][doorx + 1] = toLow(findPattern[newType][2].get(2));
						}
					}
				}
				//left side and right side
				replBase = 0;
				if(rand && jCheckerStart)
					replBase = 1;
				else if(!rand)
				{
					if(w == 7)
						replBase = 1;
					else if(w == 3)
						replBase = 2;
				}
				//left side
				//layout[y][x] = backupLayout[y][x - p[3]];
				//int tt = getTileType(layout[y][x]);
				int useList = 6;
				if(found == 2)
					useList = 8;
				if(!isSignOrPost(x, y))
				{
					layout[y][x] = toLow(findPattern[newType][useList].get(replBase));
					//System.out.println((x) + "," + y + " marked as replaced with wall L side");
					replaced[y][x] = true;
				}
				useList--;
				/*if((x + w - 1) == 8 && (y) == 28)
				{
					System.out.println(layout[y][x + w - 1]);
					System.out.println("SOP@8,28=" + isSignOrPost(x + w - 1, y));
				}*/
				if(!isSignOrPost(x + w - 1, y))
				{
					//short sh = findPattern[newType][5].get(replBase);
					layout[y][x + w - 1] = toLow(findPattern[newType][useList].get(replBase));
					//System.out.println((x + w - 1) + "," + y + " marked as replaced with wall R side");
					replaced[y][x + w - 1] = true;
				}
				if(found == 1)
				{
					if(rand)
						replBase = 0;
					//if((x + w - 1) == 8 && (y + 1) == 28)
					//	System.out.println("SOP@8,28=" + isSignOrPost(x + w - 1, y + 1));
					if(!isSignOrPost(x, y + 1))
					{
						layout[y + 1][x] = toLow(findPattern[newType][8].get(replBase));
						//System.out.println((x) + "," + (y + 1) + " marked as replaced with wall L bottom");
						replaced[y + 1][x] = true;
					}
					if(!isSignOrPost(x + w - 1, y + 1))
					{
						layout[y + 1][x + w - 1] = toLow(findPattern[newType][7].get(replBase));
						//System.out.println((x + w - 1) + "," + (y + 1) + " marked as replaced with wall R bottom");
						replaced[y + 1][x + w - 1] = true;
					}
				}
				//finally, repair roof
				if(newType == 7)
				{
					//System.out.println("Roof repair width=" + w);
					if(w == 3)
					{
						if(findPattern[7][6].contains(toHigh(layout[y - 1][x])))  //check for extra tall ME building
							y--;
						if(!isSignOrPost(x, y - 2))
							layout[y - 2][x] = toLow(findPattern[7][9].get(0));
						if(!isSignOrPost(x, y - 1))
							layout[y - 1][x] = toLow(findPattern[7][10].get(0));
						if(!isSignOrPost(x + 1, y - 2))
							layout[y - 2][x + 1] = toLow(findPattern[7][14].get(1));
						if(!isSignOrPost(x + 1, y - 1))
							layout[y - 1][x + 1] = toLow(findPattern[7][13].get(1));
						if(!isSignOrPost(x + 2, y - 2))
							layout[y - 2][x + 2] = toLow(findPattern[7][11].get(0));
						if(!isSignOrPost(x + 2, y - 1))
							layout[y - 1][x + 2] = toLow(findPattern[7][12].get(0));
					}
					else
					{
						//unmark the roof as complete
						x++;
						y -= 2;
						w -= 2;
						for(int yy = 0; yy < 2; yy++)
							for(int xx = x; xx < x + w; xx++)
								if(!isSignOrPost(xx, y + yy))
									layout[y + yy][xx] = toLow(findPattern[7][14 - yy].get(1));
						replaced[y][x] = false;
						replaced[y + 1][x] = false;
						convertSingleTile(x, y, 7, 14, brect);
						convertSingleTile(x, y + 1, 7, 13, brect);
					}
				}
			}
			else if(adr.equals("chkw"))  //this happens on tall Middle Eastern buildings with height 5 and width 3
			{
				int replBase = 0;
				if(newType == 7)
					replBase = 2;
				layout[y][x] = toLow(repl.get(replBase));
			}
			else if(adr.equals("f3mpatOR1"))  //while the value matches, if size 1, put 4, else put 0, then 1s, then 2
			{
				int x0 = x;
				while(findPattern[tileType][found].contains(val))
				{
					x++;
					val = toHigh(layout[y][x]);
				}
				x--;
				if(x0 - x == 1)
					layout[y][x0] = toLow(repl.get(3));
				else
				{
					layout[y][x0] = toLow(repl.get(0));
					x0++;
					while(x0 < x)
					{
						layout[y][x0] = toLow(repl.get(1));
						replaced[y][x0] = true;
						x0++;
					}
					layout[y][x0] = toLow(repl.get(2));
					replaced[y][x0] = true;
				}
			}
			else if(adr.equals("l/u/r"))  //choose tile based on where door is
			{
				short r = toHigh(backupLayout[y - 1][x - 1]);
				if(r == findPattern[tileType][0].get(0))
					layout[y][x] = toLow(repl.get(2));
				else
				{
					short u = toHigh(backupLayout[y - 1][x]);
					if(u == findPattern[tileType][0].get(0))
						layout[y][x] = toLow(repl.get(1));
					else
					{
						short l = toHigh(backupLayout[y - 1][x + 1]);
						if(l == findPattern[tileType][0].get(0))
							layout[y][x] = toLow(repl.get(0));
					}
				}
			}
			else if(adr.equals(">1OR1"))
			{
				int x0 = x;
				while(findPattern[tileType][found].contains(val))
				{
					x++;
					val = toHigh(layout[y][x]);
				}
				x--;
				if(x0 - x == 1)
					layout[y][x0] = toLow(repl.get(1));
				else
				{
					while(x0 <= x)
					{
						layout[y][x0] = toLow(repl.get(0));
						replaced[y][x0] = true;
						x0++;
					}
				}
			}
			else if(adr.equals("first"))
				layout[y][x] = toLow(repl.get(0));
			else if(adr.equals("f3mpat"))
			{
				int x0 = x;
				int useList = tileType;
				if(replacementPreset)
					useList = newType;
				while(findPattern[useList][found].contains(val))
				{
					x++;
					if(isSignOrPost(x, y))
						continue;
					val = toHigh(layout[y][x]);
				}
				x--;
				if(isSignOrPost(x0 - 1, y))
				{
					x0--;
				}
				if(!isSignOrPost(x0, y))
				{
					layout[y][x0] = toLow(repl.get(0));
					replaced[y][x0] = true;
				}
				x0++;
				while(x0 < x)
				{
					if(!isSignOrPost(x0, y))
					{
						layout[y][x0] = toLow(repl.get(1));
						replaced[y][x0] = true;
					}
					x0++;
				}
				if(!isSignOrPost(x0, y) && x0 <= x)
				{
					layout[y][x0] = toLow(repl.get(2));
					replaced[y][x0] = true;
				}
			}
			else if(adr.equals("fwr"))
			{
				placeForwardRoof(x, y, newType);
			}
			else if(adr.equals("trb"))
			{
				Point p = trimBuildingAt(x, y, brect);
				p.y = Math.min(y, p.y);
				//rollback processing to the start of the building
				//x = p.x;
				//y = p.y;
				//System.out.println("Resetting cursor to " + p.x + "," + p.y + " after trimming building");
				//System.out.println("After trb: At " + x + "," + y + " layout=" + layout[y][x] + "(" + toHigh(layout[y][x]) + ")");
				return p;
			}
			else if(adr.equals("t/b"))
			{
				//System.out.print("Top-bottom processing of point " + x + "," + y +"; found=" + found);
				if(y - 1 >= 0)
				{
					if(layout[y - 1][x] == toLow(repl.get(0)))
					{
						layout[y][x] = toLow(repl.get(1));
						//System.out.println(" bot");
					}
					else
					{
						layout[y][x] = toLow(repl.get(0));
						//System.out.println(" top");
					}
				}
			}
			else if(adr.equals("ich"))   //inner roof check for Middle Eastern roofs
			{
				/*int f1 = this.getTileType(layout[y][x]);
				int f2 = this.getTileType(layout[y + 1][x]);
				System.out.println("top=" + f1 + "  bot=" + f2);*/
				short test = replacePattern[newType][found - 2].get(0);
				if(layout[y - 1][x] == toLow(test))
					layout[y][x] = toLow(repl.get(0));
				else
					layout[y][x] = toLow(test);
			}
			else if(adr.equals("nr"))  //normalize roof (eliminate roof features)
			{
				//select the roof
				boolean[][] sel = selectRoof(x, y, tileType, true);
				int[] roof = {15,14,13,0,16,9,10,0,17,11,12,0,0,0,0,0};
				Rectangle rfr = getSelectedRectangle(sel);
				//replace all on roof
				//for each point
				String adrr = "";
				//for(int yy = 0; yy < 32; yy++)
				//{
					//for(int xx = 0; xx < 32; xx++)
					//{
				int corn = 0;
				if(sel[y][x])
				{
					//can I go up?   //note:signs (< 16), while not a part of the roof, do not end the roof
					if(y == 0)  corn++;
					else if(sel[y - 1][x] == false && !isRoofSign(x, y - 1, sel))  corn++;
					//can I go down?
					if(y == 31) corn |= 2;
					else if(sel[y + 1][x] == false && !isRoofSign(x, y + 1, sel))  corn |= 2;
					//can I go left?
					if(x == 0) corn |= 4;
					else if(sel[y][x - 1] == false && !isRoofSign(x - 1, y, sel)) corn |= 4;
					//can I go right?
					if(x == 31) corn |= 8;
					else if(sel[y][x + 1] == false && !isRoofSign(x + 1, y, sel))  corn |= 8;
					ArrayList<Short> roofRepl = replacePattern[newType][roof[corn]];
					if(roof[corn] == 0)
						System.out.println("Warning: processing corner value " + corn + " as 0");
					int spot = roofRepl.indexOf((short) 0) + 1;
					if(spot == 0)
					{
						layout[y][x] = toLow(roofRepl.get(0));
						//replaced[y][x] = true;
					}
					else
					{
						//int dr = repl.indexOf(0) + 1;
						short ddr = roofRepl.get(spot);
						adrr = allDirs.get(ddr);
						Point pp = convertSingleTile(x, y, newType, roof[corn], brect);
						return pp;
						//break;
						/*if(adrr.equals("first"))
							layout[y][x] = toLow(roofRepl.get(0));
						if(adrr.equals("fwr"))
						{
							placeForwardRoof(x, y, newType, replaced);
						}
						else if(adrr.equals("trb"))
						{
							Point p = trimBuildingAt(x, y, replaced);
							//rollback processing to the start of the building
							x = p.x;
							y = p.y;
						}
						else if(adrr.equals("Irf"))
						{
							placeIndianRoof(x, y, newType, replaced);
						}
						else
							System.out.println("For directive nr, the directive " + adrr + " was not addressed");*/
					}
				}
					//}
					//if(adrr.length() > 0)
						//break;
				//}
				/*if(adrr.length() > 0)
				{
					//it will either be trim building or forward feature
					
				}*/
			}
			else if(adr.equals("gr"))  //replace with grass
			{
				layout[y][x] = GRASS;
			}
			else if(adr.equals("di"))  //leftover scepter shadows - replace with dirt
			{
				layout[y][x] = 16;
			}
			else if(adr.equals("j1"))
			{
				if(y < 31)
				{
					if(layout[y + 1][x] == layout[y][x])
						layout[y + 1][x] = GRASS;
				}
				layout[y][x] = toLow(findPattern[newType][found].get(0));
			}
			else if(adr.equals("grb"))
			{
				int ss = 0;
				int[] lr = {-1,-1, -1, -1};
				int yy = y;
				int by = 0;
				boolean atEdge = false;
				if(toHigh(layout[y][x]) == findPattern[tileType][found].get(1))
				{
					layout[y + 1][x] = GRASS;
					while(isBldg(x - 1, yy, lr, false, newType))    //doing down, shade the building edge
					{
						layout[yy][x] = (byte) (GRASS + 2);
						yy++;
						atEdge = true;
					}
					yy--;
					//layout[yy][x] = (byte) (GRASS + 1);
					by = yy;
					ss = Arrays.binarySearch(shadows, layout[y + 1][x + 1]);  //unshade the previously shaded left side
					if(ss >= 0)
						layout[y + 1][x + 1] = (byte) nonShadows[ss];
				}
				yy = y;
				layout[y][x] = GRASS;
				while(isBldg(x - 1, yy, lr, false, newType))  //shade the building edge
				{
					layout[yy][x] = (byte) (GRASS + 2);
					yy--;
					atEdge = true;
				}
				yy++;
				if(atEdge)
				{
					layout[yy][x] = GRASS;
					if(y == by)
						layout[y][x] = (byte) (GRASS + 1);
				}
				ss = Arrays.binarySearch(shadows, layout[y][x + 1]);  //unshade the previously shaded left side
				if(ss >= 0)
					layout[y][x + 1] = (byte) nonShadows[ss];
			}
			else if(adr.equals("w/g"))  //Middle eastern scepter structure
			{
				//replace scepter with what is next to it
				//boolean[][] sel = selectBldg(x, y);
				//Rectangle brect = getSelectedRectangle(sel);  //get rid of ALL scepters
				boolean palace = false;
				for(int yy = brect.y; yy < brect.y + brect.height; yy++)
				{
					for(int xx = brect.x; xx < brect.x + brect.width; xx++)
					{
						int scep = getTileType(layout[yy][xx]);
						if(scep != 22)
							continue;
						y = yy;
						x = xx;
						if(buildingSelection[y][x - 1] == true)  //if left side of scepter is in building
						{
							layout[y][x] = layout[y][x - 1];
							int ttype = getTileType(backupLayout[y][x - 1]);  
							//if(ttype == 7)
								//getTileType(backupLayout[y][x - 1]);
							if(ttype >= 26)  //palace scepter
							{
								int tt2 = getTileType(backupLayout[y][1]);  //in Istanbul, copy the one at y,0
								if(tt2 >= 26)
									layout[y][x] = toLow(findPattern[4][tt2].get(0));
								else
									layout[y][x] = layout[y][x + 1];  //in Mecca; just copy the one to the right
								palace = true;  //if you find a palace scepter you must restore the backup layout before continuing
							}
							else
							{
								int[] map = {14,11,13,12,1,5,2,7};
								for(int i = 0; i < map.length; i += 2)
								{
									if(ttype == map[i])
									{
										layout[y][x] = toLow(findPattern[4][map[i + 1]].get(0));
										break;
									}
								}
							}
						}
						else if(buildingSelection[y][x + 1] == true)  //if right side in scepter is in building
						{
							layout[y][x] = layout[y][x + 1];
							int ttype = getTileType(layout[y][x + 1]);
							int[] map = {14,9,13,10,1,6,3,6,2,8,4,8};
							for(int i = 0; i < map.length; i += 2)
							{
								if(ttype == map[i])
								{
									layout[y][x] = toLow(findPattern[4][map[i + 1]].get(0));
									if(map[i] == 3 || map[i] == 4)
										layout[y][x + 1] = toLow(findPattern[4][map[i] - 2].get(0));
									break;
								}
							}
						}
						else  //left not in bldg
						{
							//if south of scepter is not in building
							if(buildingSelection[y + 1][x] == false)
							{
								//grab the one underneath and copy it to the right to get rid of the scepter shadow
								layout[y][x] = layout[y + 1][x];
								layout[y][x + 1] = layout[y][x];
							}
							else
								layout[y][x] = layout[y][x - 1];
						}
					}
				}
				if(palace)  //this function assumes a non-converted building; a palace has been partially converted, so we need to de-convert it
				{
					for(int yy = brect.y; yy < brect.y + brect.height; yy++)
					{
						for(int xx = brect.x; xx < brect.x + brect.width; xx++)
						{
							int scep = getTileType(backupLayout[yy][xx]);
							if(scep == 22)
								continue;
							layout[yy][xx] = backupLayout[yy][xx];
						}
					}
				}
				//System.out.println("Scepters in building eliminated; reset cursor to " + (brect.x - 1) + "," + brect.y + " after clearing out tiles in " + brect.toString());
				return new Point(brect.x - 1, brect.y);
			}
			else if(adr.equals("Irf"))
			{
				int[] p = bldgWidth357(x, y, 6, false, brect);
				
				if(p[0] != x)
					x = p[0];
				/*if(p[2] == -1)  //you split a building; need to go back (this never happens
				{
					y = p[1];
					x--;
					System.out.println("Resetting point to " + x + "," + y + " after Indian roof split building");
					return new Point(x, y);
				}*/
				placeIndianRoof(x, y, 6);
			}
		}
		return null;
	}
	
	private boolean bldgCanGrowLeft(Rectangle brect)
	{
		if(brect.width >= 7)
			return false;
		int[] rights = new int[brect.height];
		for(int yy = brect.y; yy < brect.y + brect.height; yy++)
		{
			int rw = 0;
			for(int xx = brect.x - 1; xx >= 0; xx--)
			{
				if(layout[yy][xx] >= 80 || layout[yy][xx] < 0)  //building
					break;
				if(layout[yy][xx] < 16)  //sign
					break;
				if(layout[yy][xx] >= 67 && layout[yy][xx] <= 75)  //water and cliffs
					break;
				if(layout[yy][xx] == 62)  //cliff edge
					break;
				if(layout[yy][xx] == 63)  //cliff edge
					break;
				rw++;
				if(rw > 4)
					break;
			}
			rights[yy - brect.y] = rw;
		}
		int min = 99;
		for(int i = 0; i < rights.length; i++)
			if(rights[i] < min)
				min = rights[i];
		if(min >= 2)
			return true;
		else
			return false;
	}
	
	private boolean bldgCanGrowDown(Rectangle brect)
	{
		//System.out.println("Test bldg grow down @" + brect.toString());
		int[] rights = new int[brect.width - 1];
		//int yy = brect.y + brect.height;
		for(int xx = brect.x; xx < brect.x + brect.width - 1; xx++)
		{
			int rw = 0;
			
			for(int yy = brect.y + brect.height; yy <= 31; yy++)
			{
				if(layout[yy][xx] >= 80 || layout[yy][xx] < 0)
				{
					int tt = getTileType(backupLayout[yy][xx]);
					if(!(tt == 22 || tt == 23 || tt > 40))  //sign stand is OK; backup layout scepter is OK; residual shadow is OK
						break;
				}
				//if(layout[yy][xx] < 16)  //sign is OK
					//break;
				if(layout[yy][xx] == 75)  //water
					break;
				rw++;
				if(rw > 4)
					break;
			}
			rights[xx - brect.x] = rw;
		}
		int min = 99;
		for(int i = 0; i < rights.length; i++)
			if(rights[i] < min)
				min = rights[i];
		if(min >= 2)
		{
			//System.out.println("true");
			return true;
		}
		else
		{
			//System.out.println("false");
			//bldgCanGrowDown(brect);
			return false;
		}
	}
	
	private boolean bldgCanGrowUp(Rectangle brect)
	{
		int[] rights = new int[brect.width];
		//int yy = brect.y + brect.height;
		for(int xx = brect.x; xx < brect.x + brect.width; xx++)
		{
			int rw = 0;
			
			for(int yy = brect.y; yy >= 0; yy--)
			{
				int tt = getTileType(layout[yy][xx]);
				if(tt == 0)  //door
					break;
				
				/*if(backupLayout[yy][xx] < 16)  //sign
					break;
				if(backupLayout[yy][xx] == 75)  //water
					break;*/
				rw++;
				if(rw > 4)
					break;
			}
			rights[xx - brect.x] = rw;
		}
		int min = 99;
		for(int i = 0; i < rights.length; i++)
			if(rights[i] < min)
				min = rights[i];
		if(min >= 2)
			return true;
		else
			return false;
	}
	
	private boolean bldgCanGrowRight(Rectangle brect)
	{
		if(brect.width >= 7)
			return false;
		int[] rights = new int[brect.height];
		for(int yy = brect.y; yy < brect.y + brect.height; yy++)
		{
			int rw = 0;
			for(int xx = brect.x + brect.width; xx <= 31; xx++)
			{
				if(layout[yy][xx] >= 80 || layout[yy][xx] < 0)  //building
					break;
				if(layout[yy][xx] < 16)  //sign
					break;
				if(layout[yy][xx] >= 67 && layout[yy][xx] <= 75)  //water and cliffs
					break;
				if(layout[yy][xx] == 62)  //cliff edge
					break;
				if(layout[yy][xx] == 63)  //cliff edge
					break;
				rw++;
				if(rw > 4)
					break;
			}
			rights[yy - brect.y] = rw;
		}
		int min = 99;
		for(int i = 0; i < rights.length; i++)
			if(rights[i] < min)
				min = rights[i];
		if(min >= 2)
			return true;
		else
			return false;
	}
	
	private void shiftBldgLSide(Rectangle brect, int amt, int newTileType)
	{
		int xx = brect.x;
		if(amt > 0)  //shifting right (making bldg smaller)
		{
			//int[] shadows =    {17, 18, 20, 21, 23, 24, 26, 27, 29, 30};
			//int[] shadowBase = {16, 16, 19, 19, 22, 22, 25, 25, 28, 28};
			for(int yy = brect.y; yy < brect.y + brect.height; yy++)
			{
				if(layout[yy][xx] >= 16 && layout[yy][xx] < 80)  //do not copy the bottom of wings
				{
					while(yy > brect.y)  //restore everything up to the top wall
					{
						layout[yy][xx + amt] = backupLayout[yy][xx + amt];
						yy--;
					}
					break;
				}
				if(!isSignOrPost(xx + amt, yy) && !isSignOrPost(xx, yy))  //if the destination isn't sign or post
					layout[yy][xx + amt] = layout[yy][xx];   //bldg egde + amt = bldg edge (moves -->)
			}
			while(amt > 0)
			{
				amt--;
				for(int yy = brect.y; yy < brect.y + brect.height; yy++)
				{
					if(!isSignOrPost(xx + amt, yy))
					{
						//if(xx == 0)
						//{
							layout[yy][xx] = GRASS;
							//continue;
						/*}
						layout[yy][xx + amt] = layout[yy][xx - 1];  //copy L of building left
						if(layout[yy][xx + amt] == 63 || layout[yy][xx + amt] == 62)
							layout[yy][xx + amt] = GRASS;
						//but unshadowed
						int f = Arrays.binarySearch(shadows, layout[yy][xx + amt]);
						if(f >= 0)
							layout[yy][xx + amt] = (byte) (nonShadows[f]);*/
					}
					/*else
					{
						layout[yy][xx + amt + 1] = layout[yy][xx + amt];  //source is sign so copy it
						layout[yy][xx] = GRASS;
						if(isSignOrPost(xx + amt, yy + 1))
						{
							layout[yy + 1][xx + amt + 1] = layout[yy + 1][xx + amt];
							layout[yy + 1][xx] = GRASS;
						}
					}*/
				}
			}
		}
		else
		{
			int growLimit = 0;
			for(int yy = brect.y; yy < brect.y + brect.height; yy++)
			{
				if(!isSignOrPost(xx + amt, yy))
				{
					if(!isSignOrPost(xx, yy))
						layout[yy][xx + amt] = layout[yy][xx];  //bldg edge - amt = bldg edge (moves <--)
					else
						layout[yy][xx + amt] = selectBldgEdge(xx + 2, yy, true);
				}
				if(layout[yy][xx] >= 16 && layout[yy][xx] < 80)  //done copying
					break;
				else
					growLimit++;
			}
			while(amt < 0)  //shifting left (making bldg larger)
			{
				amt++;
				if(growLimit < brect.height)  //put a standard 2-roof, 2-wall
				{
					//newTileType should be 6 for this, leave roof as old style 
					//to allow placeIndianRoof() to replace it all later
					int[] tps = {13, 14, 1, 2};
					for(int yy = brect.y; yy < brect.y + 4; yy++)  
					{
						layout[yy][xx + amt] = toLow(findPattern[tileType][tps[yy - brect.y]].get(0));
					}
				}
				else
				{
					for(int yy = brect.y; yy < brect.y + brect.height; yy++)
					{
						int tt = getTileType(layout[yy][xx + 1]);
						if(tt > 0 && !isSignOrPost(xx + amt, yy))  
							layout[yy][xx + amt] = layout[yy][xx + 1];
						//else  //door or sign
							//layout[yy][xx + amt] = selectBldgFill(xx + 2, yy, tileType);
					}
				}
			}
		}
	}
	
	private byte selectBldgEdge(int x, int y, boolean growingLeft)  
	{
		int nt = -1;
		while(nt == -1)
		{
			int tt = getTileType(layout[y][x]); 
			switch(tt)
			{
			case 1:  case 5:  case 6:
				if(growingLeft)
					nt = 6;
				else
					nt = 5;
				break;
			case 2:  case 7:  case 8:
				if(growingLeft)
					nt = 8;
				else
					nt = 7;
				break;
			default:
				if(growingLeft)
					x++;
				else
					x--;
			}
		}
		return toLow(findPattern[tileType][nt].get(0));
	}
	
	private byte selectBldgFill(int x, int y, int newTileType)
	{
		if(newTileType == 6)
			newTileType = tileType;
		int tt = getTileType(layout[y][x], newTileType);  //wall, btm wall, 
		switch(tt)
		{
		case 1:  case 5:  case 6:
			return toLow(findPattern[newTileType][1].get(0));
		case 2:  case 7:  case 8: 
			return toLow(findPattern[newTileType][2].get(0));
		case 9:  case 11:  case 13:
			return toLow(findPattern[newTileType][13].get(0));
		case 10:  case 12:  case 14:
			return toLow(findPattern[newTileType][14].get(0));
		case 20:
			if(findPattern[newTileType][20].get(0) == toHigh(layout[y][x]))
				return toLow(findPattern[newTileType][14].get(0));
			else
				return toLow(findPattern[newTileType][13].get(0));
		default:
			//System.out.println("Failed to find appropriate fill for tt=" + tt + " at " + x + "," + y);  //center roof isn't available
			return toLow(findPattern[tileType][1].get(0));
		}
	}
	
	private void growBldgUp(Rectangle brect, int newTileType)
	{
		//shift everything up
		int wally = brect.y;
		
		for(int yy = brect.y; yy < brect.y + brect.height - 1; yy++)
		{
			for(int xx = brect.x; xx < brect.x + brect.width; xx++)
			{
				layout[yy - 1][xx] = layout[yy][xx];
			}
			wally = yy;
		}
		wally++;
		//insert a normal row of bottom wall
		layout[wally][brect.x] = toLow(findPattern[tileType][6].get(0));
		for(int xx = brect.x + 1; xx < brect.x + brect.width - 2; xx++)
			layout[wally][xx] = toLow(findPattern[tileType][1].get(0));
		layout[wally][brect.x + brect.width - 1] = toLow(findPattern[tileType][5].get(0));
		//boolean[][] done = new boolean[32][32];
		//for(int xx = brect.x; xx < brect.x + brect.width; xx++)
			//convertSingleTile(xx, wally, newTileType, -1, done, brect);
	}
	
	private void hutToBldgPostProcess(Rectangle brect, int newTileType)  
	{
		//remove the bottom of each building
		int yrem = brect.y + brect.height - 1;
		for(int xx = brect.x; xx < brect.x + brect.width; xx++)
			layout[yrem][xx] = layout[yrem + 1][xx];
		brect.height--;
		if(brect.height < 4)
		{
			while(bldgCanGrowUp(brect))
			{
				growBldgUp(brect, newTileType);
				brect.height++;
				brect.y--;
				if(brect.height >= 4)
					if(UWNHRando.rand() < 0.5 + 0.2 * (brect.height - 4))
						break;
			}
		}
	}
	
	private void shiftBldgRSide(Rectangle brect, int amt, int newTileType)
	{
		//int[] shadows =    {17, 18, 20, 21, 23, 24, 26, 27, 29, 30};
		//int[] shadowBase = {16, 16, 19, 19, 22, 22, 25, 25, 28, 28};
		int xx = brect.x + brect.width - 1;
		if(amt > 0)   //shifting right (making bldg larger)
		{
			int growLimit = 0;
			for(int yy = brect.y; yy < brect.y + brect.height; yy++)
			{
				if(isSignOrPost(xx + amt, yy))
					continue;
				if(isSignOrPost(xx, yy))
					layout[yy][xx + amt] = selectBldgEdge(xx - 2, yy, false);
				else
					layout[yy][xx + amt] = layout[yy][xx];  //bldg edge + amt = bldg edge (moves -->)
				//shadow checking (this puts the shadow)
				if(layout[yy][xx] >= 80 || layout[yy][xx] < 16)
				{
					int f = Arrays.binarySearch(nonShadows, layout[yy][xx + amt + 1]);
					if(f >= 0 && xx + amt + 1 < 32)
					{
						if(yy == brect.y)
							layout[yy][xx + amt + 1] = (byte) nonShadows[f];
						else if(yy == brect.y + brect.height - 1)
							layout[yy][xx + amt + 1] = (byte) (nonShadows[f]  + 1);
						else
							layout[yy][xx + amt + 1] = (byte) (nonShadows[f] + 2);
					}
					growLimit++;
				}
				else
				{
					int f = Arrays.binarySearch(shadows, layout[yy - 1][xx + amt + 1]);
					if(f >= 0 && xx + amt + 1 < 32)
						layout[yy - 1][xx + amt + 1] = (byte) (nonShadows[f] + 1);
					break;
				}
			}
			while(amt > 0)
			{
				amt--;
				if(growLimit < brect.height)  //put a standard 2-roof, 2-wall
				{
					//newTileType should be 6 for this, leave roof as old style 
					//to allow placeIndianRoof() to replace it all later
					int[] tps = {13, 14, 1, 2};
					for(int yy = brect.y; yy < brect.y + 4; yy++)  
					{
						if(yy - brect.y < 4)
							layout[yy][xx + amt] = toLow(findPattern[tileType][tps[yy - brect.y]].get(0));
						else
							layout[yy][xx + amt] = GRASS;
					}
				}
				else
				{
					for(int yy = brect.y; yy < brect.y + brect.height; yy++)
					{
						int tt = getTileType(layout[yy][xx - 1]);
						if(tt > 0 && !isSignOrPost(xx + amt, yy))  
							layout[yy][xx + amt] = layout[yy][xx - 1];
						//else
							//layout[yy][xx + amt] = selectBldgFill(xx - 2, yy, tileType);
					}
				}
			}
		}
		else
		{
			int wingBottom = brect.y + brect.height - 1;
			for(int yy = brect.y; yy < brect.y + brect.height; yy++)
			{
				if(layout[yy][xx] >= 16 && layout[yy][xx] < 80)  //do not copy the bottom of wings
				{
					wingBottom = yy - 1;
					while(yy > brect.y)  //restore everything up to the top wall
					{
						layout[yy][xx + amt] = backupLayout[yy][xx + amt];
						yy--;
					}
					break;
				}
				if(!isSignOrPost(xx + amt, yy) && !isSignOrPost(xx, yy))
					layout[yy][xx + amt] = layout[yy][xx];   //bldg edge - amt = bldg edge (moves <--)
				else
				{
					int f = Arrays.binarySearch(shadows, layout[yy][xx + 1]);
					if(f >= 0)
						layout[yy][xx + 1] = (byte) nonShadows[f];
				}
			}
			boolean shadowGone = false;
			while(amt < 0)  //shifting left (bldg becomes smaller)
			{
				amt++;
				for(int yy = brect.y; yy < brect.y + brect.height; yy++)
				{
					if(isSignOrPost(xx + amt, yy))
					{
						/*layout[yy][xx + amt + 1] = layout[yy][xx + amt];  //source is sign so copy it
						layout[yy][xx] = GRASS;
						if(isSignOrPost(xx + amt, yy + 1))
						{
							layout[yy + 1][xx + amt + 1] = layout[yy + 1][xx + amt];
							layout[yy + 1][xx] = GRASS;
						}*/
						continue;
					}
					else
					{
						if(yy == brect.y)
							layout[yy][xx + amt] = GRASS;
						else if(yy == wingBottom)
							layout[yy][xx + amt] = (byte) (GRASS + 1);
						else
							layout[yy][xx + amt] = (byte) (GRASS + 2);
						if(xx < 31 && !shadowGone)
						{
							int f = Arrays.binarySearch(shadows, layout[yy][xx + 1]);
							if(f >= 0)
								layout[yy][xx + 1] = (byte) nonShadows[f];
						}
					}
					/*}
					layout[yy][xx + amt] = layout[yy][xx + 1];
					if(layout[yy][xx + amt] == 63 || layout[yy][xx + amt] == 62)
						layout[yy][xx + amt] = GRASS;
					if(!shadowGone)
					{
						
					}*/
				}
				shadowGone = true;
			}
		}
	}
	
	private Point[] getDoors(Rectangle brect, boolean verbose)
	{
		Point[] rv = new Point[2];
		for(int yy = brect.y; yy < brect.y + brect.height; yy++)
		{
			for(int xx = brect.x; xx < brect.x + brect.width; xx++)
			{
				if(verbose)
					System.out.print(layout[yy][xx] + ":" + Integer.toHexString(toHigh(layout[yy][xx])) + "," + getTileType(layout[yy][xx]) + ",");
				if(getTileType(layout[yy][xx]) == 0)
				{
					if(rv[0] == null)
						rv[0] = new Point(xx, yy);
					else
						rv[1] = new Point(xx, yy);
				}
			}
			if(verbose)
				System.out.println();
		}
		return rv;
	}
	
	//we are trying to avoid this problematic function
	private Rectangle[] splitBldg(Rectangle brect, Point[] doors)  //note: this is used with buildings with normalized roofs only
	{
		int splitX = doors[0].x + 1;
		Rectangle r1 = null;
		Rectangle r2 = null;
		//(x2-x1+1)=w
		if(doors[0].x == doors[1].x)
		{
			r1 = new Rectangle(brect.x, brect.y, splitX - brect.x + 1, brect.height);
			r2 = new Rectangle(splitX + 1, brect.y, (brect.width + brect.x) - (splitX + 1), brect.height);
		}
		else
		{
			System.out.println("Touching bldgs");
		}
		//rollback changes on building
		for(int yy = brect.y; yy < brect.y + brect.height; yy++)
		{
			for(int xx = brect.x; xx < brect.x + brect.width; xx++)
			{
				layout[yy][xx] = backupLayout[yy][xx];
				replaced[yy][xx] = false;
			}
		}
		//look at splitX
		int[] rsSplitFind = {13,14,1,2};  //roof top, roof bottom, wall top, wall bottom
		int[] rsSplitRepl = {11,12,5,7};  //roof 7, roof J, wall R, wall bottom R
		int[] lsSplitFind = {13,14,1,2};  //roof top, roof bottom, wall top, wall bottom
		int[] lsSplitRepl = {9,10,6,8};  //roof P, roof L, wall L, wall bottom L
		for(int yy = brect.y; yy < brect.y + brect.height; yy++)
		{
			int tt1 = getTileType(layout[yy][splitX]);
			for(int i = 0; i < rsSplitFind.length; i++)
			{
				if(rsSplitFind[i] == tt1)
				{
					layout[yy][splitX] = toLow(findPattern[tileType][rsSplitRepl[i]].get(0));
				}
			}
			int tt2 = getTileType(layout[yy][splitX + 1]);
			for(int i = 0; i < rsSplitFind.length; i++)
			{
				if(lsSplitFind[i] == tt2)
				{
					layout[yy][splitX + 1] = toLow(findPattern[tileType][lsSplitRepl[i]].get(0));
				}
			}
		}
		Rectangle[] rv = {r1, r2};
		return rv;
	}
	
	/*private void shadowPass(int newTileType)
	{
		int[] shadows = {17, 18, 20, 21, 23, 24, 26, 27, 29, 30};
		int[] low = {16, 16, 19, 19, 22, 22, 25, 25, 28, 28};
		int[] lrf = {-1,-1};
		for(int yy = 0; yy < 32; yy++)
		{
			for(int xx = 0; xx < 32; xx++)
			{
				int f = Arrays.binarySearch(shadows, layout[yy][xx]);
				if(f > 0)
				{
					int g = Arrays.binarySearch(shadows, layout[yy][xx]);
					if(g > 0)
						continue;
					if(isBldg(xx - 1, yy, lrf, false, newTileType))
					{
						if(!isBldg(xx - 1, yy - 1, lrf, false, newTileType))  //turn off shadow
							layout[yy][xx] = (byte) low[f];
						else if(!isBldg(xx - 1, yy + 1, lrf, false, newTileType))  //half shadow
							layout[yy][xx] = (byte) (low[f] + 1);
						else
							layout[yy][xx] = (byte) (low[f] + 2);  //full shadow
					}
				}
			}
		}
	}*/
	
	private int[] bldgWidthMax5(int xx, int yy, int newTileType)  //enforces a building width max of 5, centered around door
	{
		//System.out.println("attempting bwm5 at " + xx + "," + yy);
		int[] rv = {xx, yy, -1};
		
		//the building has been pre-trimmed before this
		boolean[][] bldg = null;
		if(tileType != 8)
		{
			bldg = selectRoof(xx, yy - 1, newTileType, false);  //this gets the width
			if(bldg == null)
				bldg = selectRoof(xx + 1, yy - 1, newTileType, false);
		}
		else
		{
			bldg = selectRoof(xx, yy - 1, tileType, true);  //account for overlap between Japanese walls and huts
		}
		Rectangle brect = getSelectedRectangle(bldg);
		/*boolean[][] bldgf = selectBldg(xx, yy);  //this gets the height
		if(bldgf == null)
			bldgf = selectBldg(xx + 1, yy);
		Rectangle frect = getSelectedRectangle(bldgf);
		int nh = frect.height + frect.y - brect.y; 
		brect.height = nh;*/
		if(brect.width < 3)  //not a building
		{
			rv[0] = -1;
			return rv;
		}
		if(tileType == 3 && brect.width == 7)  //preserve width 7 buildings for other hut
		{
			rv[2] = 7;
			return rv;
		}
		//temporarily set this as high to find the door; it should be found above this point
		brect.height += 4;  
		if(brect.y + brect.height > 32)  //but don't go beyond 32
			brect.height = 32 - brect.y;
		//System.out.println("Calling bldgWidthMax5 on building at " + brect.toString());
		/*if(brect.x == 12 && brect.y == 22)
			System.out.println();*/
		Point[] doors = getDoors(brect, false);  //this gets the height
		/*if(doors[1] != null)  //you must split the building
		{
			splitBldg(brect, doors);
			rv[0] = brect.x;
			rv[1] = brect.y;
			return rv;
		}*/
		if(tileType == 3 || tileType == 5)  //door = 1 above building bottom
			brect.height = (doors[0].y - brect.y) + 2;
		else
			brect.height = (doors[0].y - brect.y) + 1;   //door is on building bottom
		Point signPoint = null;
		//System.out.println("Found door at " + doors[0].toString());
		if(yy < doors[0].y)  //move the roof down to above the door
		{
			int dr = 0;  //stops here on type 3,5
			int[] lrf = {-1,-1};
			boolean rFound = false;
			while(rFound == false)
			{
				dr++;
				for(int rx = brect.x; rx < brect.x + brect.width; rx++)
				{
					if(tileType != 8)
					{
						if(isRoof(rx, doors[0].y - dr, lrf, newTileType, false) == true)
						{
							rFound = true;
							break;
						}
					}
					else
					{
						if(isRoof(rx, doors[0].y - dr, lrf, tileType, true) == true)
						{
							rFound = true;
							break;
						}
					}
					if(layout[doors[0].y - dr][rx] >= 0 && layout[doors[0].y - dr][rx] < 16)
						signPoint = new Point(rx, doors[0].y - dr);
				}
			}
			if(dr > 1)
			{
				
				int sy = doors[0].y - dr;
				//System.out.println("Door at y=" + doors[0].y + " (x="  + doors[0].x + ")" +  "  sy=" + sy);
				if(signPoint != null)
				{
					if(signPoint.x == doors[0].x)
					{
						layout[sy][doors[0].x] = layout[signPoint.y][signPoint.x];
						//System.out.println("Moved sign from " + signPoint.toString() + " to " + signPoint.x + "," + sy);
					}
					else
					{
						layout[doors[0].y][signPoint.x] = layout[signPoint.y][signPoint.x];	
						//System.out.println("Moved sign from " + signPoint.toString() + " to " + signPoint.x + "," + doors[0].y);
					}
				}
				for(int ry = doors[0].y - 1; sy >= brect.y; ry--)
				{
					//System.out.println("Shifting line " + sy + " to " + ry);
					for(int rx = brect.x; rx < brect.x + brect.width; rx++)
					{
						layout[ry][rx] = layout[sy][rx];
						//replaced[ry][rx] = true;
					}
					if(sy == brect.y && (brect.x + brect.width) < 32)  //unshadow brect.x + brect.width, sy
					{
						int f = Arrays.binarySearch(shadows, layout[ry][brect.x + brect.width]);
						if(f >= 0)
							layout[ry][brect.x + brect.width] = (byte) nonShadows[f];
					}
					sy--;
				}
				dr--;
				if(brect.y > dr)
				{
					for(int i = dr - 1; i >= 0; i--)
					{
						for(int rx = brect.x; rx < brect.x + brect.width; rx++)
						{
							layout[brect.y + i][rx] = layout[brect.y - 1][rx];
							//replaced[brect.y + i][rx] = true;
						}
						//unshadow brect.x + brect.width, sy
						if(brect.x + brect.width < 32)
						{
							int f = Arrays.binarySearch(shadows, layout[brect.y + i][brect.x + brect.width]);
							if(f >= 0)
								layout[brect.y + i][brect.x + brect.width] = (byte) nonShadows[f];
						}
					}
				}
				else
				{
					for(int i = 0; i <= brect.y; i++)
					{
						for(int rx = brect.x; rx <= brect.x + brect.width; rx++)
						{
							replaced[i][rx] = true;
							layout[i][rx] = GRASS;
						}
						if(brect.x + brect.width < 32)
						{
							int f = Arrays.binarySearch(shadows, layout[i][brect.x + brect.width]);
							if(f >= 0)
								layout[i][brect.x + brect.width] = (byte) nonShadows[f];
						}
					}
				}
				brect.y += dr;
				brect.height -= dr;
			}
			
			yy = doors[0].y;
		}
		for(int rx = brect.x + 1; rx < brect.x + brect.width - 1; rx++)
		{
			byte bb = layout[brect.y + 1][rx];
			if(bb > 16 && bb != 84)
				System.out.println("Copy error in roof shift down");
		}
		int dl = doors[0].x - brect.x;
		int dr = brect.x + brect.width - doors[0].x - 1;
		Point[] dc2 = getDoors(brect, false);
		if(dc2[0] == null)
			System.out.println("Lost the door after shifting roof down");
		while(dl >= dr + 2)  //off center door to left - cut left
		{
			shiftBldgLSide(brect, 1, newTileType);
			dl--;
			brect.width--;
			brect.x++;
		}
		while(dr >= dl + 2)  //off center door to right - cut right
		{
			shiftBldgRSide(brect, -1, newTileType);
			dr--;
			brect.width--;
		}
		dc2 = getDoors(brect, false);
		if(dc2[0] == null)
			System.out.println("Lost the door after initial building reduction");
		rv[0] = brect.x;
		rv[1] = yy;
		rv[2] = brect.width;
		switch(brect.width)
		{
		case 3: 
			if(dl == dr)
			{
			//	System.out.println("Building rect now " + brect.toString());
				return rv;
			}
			else
			{
				System.out.println("Door on side in bldg size 3 - don't know what to do");
				return null;
			}
		case 4:  case 6:  case 8:
			while(rv[2] != 5 && rv[2] != 3)
			{
				if(dl < dr)  //left side is smaller
				{
					if(bldgCanGrowLeft(brect) && rv[2] < 5)
					{
						shiftBldgLSide(brect, -1, newTileType);
						rv[0]--;
						rv[2]++;
						//rv[3]--;
						brect.x--;
						brect.width++;
						//return rv;
					}
					else
					{
						shiftBldgRSide(brect, -1, newTileType);
						//rv[0]++;
						rv[2]--;
						//rv[4]--;
						brect.width--;
						//return rv;
					}
				}
				else  //right side is smaller
				{
					if(bldgCanGrowRight(brect) && rv[2] < 5)
					{
						shiftBldgRSide(brect, 1, newTileType);
						rv[2]++;
						//rv[4]++;
						brect.width++;
						//return rv;
					}
					else
					{
						shiftBldgLSide(brect, 1, newTileType);
						rv[0]++;
						rv[2]--;
						//rv[3]++;
						brect.x++;
						brect.width--;
						//return rv;
					}
				}
			}
			dc2 = getDoors(brect, false);
			if(dc2[0] == null)
				System.out.println("Lost the door after final building reduction");
			/*for(int rx = brect.x + 1; rx < brect.x + brect.width - 1; rx++)
			{
				byte bb = layout[brect.y + 1][rx];
				if(bb > 16 && bb != 84)
					System.out.println("Copy error in building shrink");
			}*/
			//System.out.println("Building rect now " + brect.toString());
			return rv;
		case 5: 
			//System.out.println("Building rect now " + brect.toString());
			return rv;
		case 7:  case 9:
			while(rv[2] > 5)
			{
				shiftBldgRSide(brect, -1, newTileType);
				shiftBldgLSide(brect, 1, newTileType);
				rv[2] -= 2;
				rv[0]++;
				brect.x++;
				brect.width -= 2;
			}
			for(int rx = brect.x + 1; rx < brect.x + brect.width - 1; rx++)
			{
				byte bb = layout[brect.y + 1][rx];
				if(bb > 16 && bb != 84)
					System.out.println("Copy error in building shrink");
			}
			//System.out.println("Building rect now " + brect.toString());
			return rv;
		}
		//9 is the widest non-palace building
		return null;
	}
	
	private boolean[] bldgWingCheck(Rectangle brect)  //return true if a building wing is even; false otherwise
	{
		//left wing
		int lw = 0;
		boolean endWing = false;
		for(int xx = 0; xx < brect.width; xx++)
		{
			for(int yy = brect.height - 1; yy > 0; yy--)
			{
				byte vv = layout[yy + brect.y][xx + brect.x];
				if(vv >= 0 && vv < 16)
					continue;
				if(vv >= 80 || vv < 0)
				{
					endWing = true;
					break;
				}
				lw++;
				break;
			}
			if(endWing)
				break;
		}
		int rw = 0;
		endWing = false;
		for(int xx = brect.width - 1; xx >= 0; xx--)
		{
			for(int yy = brect.height - 1; yy > 0; yy--)
			{
				byte vv = layout[yy + brect.y][xx + brect.x];
				if(vv >= 0 && vv < 16)
					continue;
				if(vv >= 80 || vv < 0)
				{
					endWing = true;
					break;
				}
				rw++;
				break;
			}
			if(endWing)
				break;
		}
		boolean[] rv = {true, true};
		if((lw & 1) == 1)
			rv[0] = false;
		if((rw & 1) == 1)
			rv[1] = false;
		return rv;
	}
	
	private int[] bldgWidth357(int xx, int yy, int newTileType, boolean centerDoor, Rectangle brect) //enforces a building width of 3, 5, or 7
	{
		int[] rv = {xx, yy, -1, 0, 0};
		//boolean[][] bldg = selectBldg(xx, yy);
		//Rectangle brect = getSelectedRectangle(bldg);
		//System.out.println("3-5-7 building resize on building at " + brect.toString());
		if(brect.width < 3)  //not a building - this should never happen!!!
			return null;
		Point[] doors = getDoors(brect, false);
		if(doors[1] != null)  //do not split the building; just return w=7 and rv[3] = first door x
		{
			//splitBldg(brect, doors);
			rv[0] = brect.x;
			rv[1] = brect.y;
			rv[2] = 7;
			rv[3] = doors[0].x - brect.x;
			return rv;
		}
		/*if(doors[0] == null)
		{
			System.out.println("No door was found for building at " + xx + ", " + yy + " brect=" + brect.toString()); 
			getDoors(brect, true);
		}*/
		int dl = doors[0].x - brect.x;
		int dr = brect.x + brect.width - doors[0].x - 1;
		boolean growLarger = false;
		boolean shrinkSmaller = false;
		//BuildingView bw = new BuildingView(brect, false);
		//bw.setTitle("Bldg at " + brect.toString() + ": Before");
		if(centerDoor)  //door must be in CENTER of building
		{
			while(dl >= dr + 2)  //off center door to left - cut left
			{
				shiftBldgLSide(brect, 1, newTileType);
				brect.x++;
				brect.width--;
				//rv[3]++;
				dl--;
			}
			while(dr >= dl + 2)  //off center door to right - cut right
			{
				shiftBldgRSide(brect, -1, newTileType);
				brect.width--;
				//rv[4]--;
				dr--;
			}
		}
		else if(dl != dr)  //random growth!
		{
			if(newTileType == 6)  //do a wing check to prevent uneven growth of a wing
			{
				boolean[] wg = bldgWingCheck(brect);
				//you must grow in the direction of the uneven wing
				if(dl > dr) //attempt to grow right
					growLarger = wg[1];  //if right wing is even, you MUST grow left larger, else you MUST NOT grow larger
				else if(dr > dl)
					growLarger = wg[0];  //if left wing is even, you MUST grow right larger else you MUST NOT grow larger
				//else if(UWNHRando.rand() > 0.5)
					//growLarger = true;   //if neither of the above, random
				//if(wg[0] != wg[1])
				shrinkSmaller = !growLarger;  //enforce that if you cannot grow the large side, you MUST shrink the small side
			}
			else if(UWNHRando.rand() > 0.5)  //50% chance to grow the larger side if able
				growLarger = true;
		}
		rv[0] = brect.x;
		rv[1] = yy;
		rv[2] = brect.width;
		switch(brect.width)
		{
		case 3: 
			if(dl == dr)
				return rv;
			else
			{
				System.out.println("Door on side in bldg size 3 - don't know what to do");
				return null;
			}
		case 4:  case 6:  case 8:
			if(growLarger)
			{
				if(dl < dr)  //grow right
				{
					if(bldgCanGrowRight(brect))
					{
						shiftBldgRSide(brect, 1, newTileType);
						rv[2]++;
						//rv[4]++;
						brect.width++;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
					else  //shrink right
					{
						shiftBldgRSide(brect, -1, newTileType);
						//rv[0]++;
						rv[2]--;
						//rv[4]--;
						brect.width--;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
				}
				else  //grow left
				{
					if(bldgCanGrowLeft(brect))
					{
						shiftBldgLSide(brect, -1, newTileType);
						rv[0]--;
						rv[2]++;
						//rv[3]--;
						brect.x--;
						brect.width++;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
					else  //shrink left
					{
						shiftBldgLSide(brect, 1, newTileType);
						rv[0]++;
						rv[2]--;
						//rv[3]++;
						brect.x++;
						brect.width--;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
				}
			}
			if(shrinkSmaller)  
			{
				if(dl > dr)  //grow right
				{
					if(bldgCanGrowRight(brect))
					{
						shiftBldgRSide(brect, 1, newTileType);
						rv[2]++;
						//rv[4]++;
						brect.width++;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
					else  //shrink right
					{
						shiftBldgRSide(brect, -1, newTileType);
						//rv[0]++;
						rv[2]--;
						//rv[4]--;
						brect.width--;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
				}
				else  //grow left
				{
					if(bldgCanGrowLeft(brect))
					{
						shiftBldgLSide(brect, -1, newTileType);
						rv[0]--;
						rv[2]++;
						//rv[3]--;
						brect.x--;
						brect.width++;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
					else
					{
						shiftBldgLSide(brect, 1, newTileType);
						rv[0]++;
						rv[2]--;
						//rv[3]++;
						brect.x++;
						brect.width--;
						//BuildingView bw2 = new BuildingView(brect, true);
						//bw2.setTitle("Bldg at " + brect.toString() + ": After");
						return rv;
					}
				}
			}
			if(dl < dr)  //left side is smaller
			{
				if(bldgCanGrowLeft(brect))
				{
					shiftBldgLSide(brect, -1, newTileType);
					rv[0]--;
					rv[2]++;
					//rv[3]--;
					brect.x--;
					brect.width++;
					//BuildingView bw2 = new BuildingView(brect, true);
					//bw2.setTitle("Bldg at " + brect.toString() + ": After");
					return rv;
				}
				else
				{
					shiftBldgRSide(brect, -1, newTileType);
					//rv[0]++;
					rv[2]--;
					//rv[4]--;
					brect.width--;
					//BuildingView bw2 = new BuildingView(brect, true);
					//bw2.setTitle("Bldg at " + brect.toString() + ": After");
					return rv;
				}
			}
			else  //right side is smaller
			{
				if(bldgCanGrowRight(brect))
				{
					shiftBldgRSide(brect, 1, newTileType);
					rv[2]++;
					//rv[4]++;
					brect.width++;
				//	BuildingView bw2 = new BuildingView(brect, true);
					//bw2.setTitle("Bldg at " + brect.toString() + ": After");
					return rv;
				}
				else
				{
					shiftBldgLSide(brect, 1, newTileType);
					rv[0]++;
					rv[2]--;
					//rv[3]++;
					brect.x++;
					brect.width--;
					//BuildingView bw2 = new BuildingView(brect, true);
					//bw2.setTitle("Bldg at " + brect.toString() + ": After");
					return rv;
				}
			}
		case 5:  case 7:
			return rv;
		case 9:
			if(newTileType == 6 && index == 13)  //for Indian roofs do not touch the 9-wide building in Venice
				return rv;
			shiftBldgRSide(brect, -1, newTileType);
			shiftBldgLSide(brect, 1, newTileType);
			rv[0]++;
			rv[2] -= 2;
			//rv[3]++;
			//rv[4]--;
			brect.x++;
			brect.width -= 2;
			//BuildingView bw2 = new BuildingView(brect, true);
			//bw2.setTitle("Bldg at " + brect.toString() + ": After");
			return rv;
		}
		//9 is the widest non-palace building
		return null;
	}

	public boolean convertTileType(int newType, PortWindow portWindow)
	{
		if(newType == origTileType)
			return false;
		if(isCapital)  //capitals must be converted to a capital type
		{
			if(!(newType == 0 || newType == 1 || newType == 4))
				return false;
		}
		backup();   //some replacement directives will require referencing the backup layout
		replaced = new boolean[32][32];
		if(tileType == 4)
			GRASS = 16;
		else if(tileType == 6)
			GRASS = 19;
		else
			GRASS = 22;
		portDoors.clear();
		fixPortConversionProblems();
		goingBack = false;
		if(hasHighFloor[newType] && !hasHighFloor[tileType])  //converting from hut?  go backwards
		{
			goingBack = true;
			for(int y = 31; y >= 0; y--)
			{
				for(int x = 31; x >= 0; x--)
				{
					if(replaced[y][x])
						continue;
					boolean[][] bldg = selectBldg(x, y);
					buildingSelection = bldg;
					if(bldg == null)
					{
						convertSingleTile(x, y, newType, -1, null);
						continue;
					}
					Rectangle brect = getSelectedRectangle(bldg);
					//System.out.println("Processing building at " + brect.toString());
					//if(brect.width >= 5)
						//selectBldg(x, y);
					Point[] doors = getDoors(brect, false);
					for(int i = 0; i < doors.length; i++)
					{
						if(doors[i] != null)
							if(!portDoors.contains(doors[i]))
								portDoors.add(doors[i]);
					}
					Point p = convertBuilding(brect, newType, portWindow);
					//Point p = convertSingleTile(x, y, newType, -1, replaced);
					if(p != null)
					{
						x = p.x;
						y = p.y;
						//System.out.println("Setting cursor to " + x + "," + y);
					}
					else
					{
						for(int yy = brect.y; yy < brect.y + brect.height; yy++)
							for(int xx = brect.x; xx < brect.x + brect.width; xx++)
								replaced[yy][xx] = true;
						//x = brect.x + brect.width;
					}
					if(portWindow != null)
					{
						portWindow.pan.setPort(this, newType, false);
						portWindow.pan.paintImmediately(0,0,800,800);
					}
				}
			}
		}
		else
		{
			for(int y = 0; y < 32; y++)
			{
				for(int x = 0; x < 32; x++)
				{
					if(replaced[y][x])
						continue;
					boolean[][] bldg = selectBldg(x, y);
					buildingSelection = bldg;
					if(bldg == null)
					{
						convertSingleTile(x, y, newType, -1, null);
						continue;
					}
					Rectangle brect = getSelectedRectangle(bldg);
					//System.out.println("Processing building at " + brect.toString());
					//if(brect.width >= 5)
						//selectBldg(x, y);
					Point[] doors = getDoors(brect, false);
					for(int i = 0; i < doors.length; i++)
					{
						if(doors[i] != null)
							if(!portDoors.contains(doors[i]))
								portDoors.add(doors[i]);
					}
					Point p = convertBuilding(brect, newType, portWindow);
					//Point p = convertSingleTile(x, y, newType, -1, replaced);
					if(p != null)
					{
						x = p.x;
						y = p.y;
						//System.out.println("Setting cursor to " + x + "," + y);
					}
					else
					{
						for(int yy = brect.y; yy < brect.y + brect.height; yy++)
							for(int xx = brect.x; xx < brect.x + brect.width; xx++)
								replaced[yy][xx] = true;
						//x = brect.x + brect.width;
					}
					if(portWindow != null)
					{
						portWindow.pan.setPort(this, newType, false);
						portWindow.pan.paintImmediately(0,0,800,800);
					}
				}
			}
		}
		return true;
	}
	
	public int getTileType(byte in)
	{
		short testval = toHigh(in);
		for(int k = 0; k < findPattern[tileType].length; k++)
		{
			if(findPattern[tileType][k].contains(testval))
				return k;
		}
		return -1;
	}
	
	public int getTileType(byte in, int newType)
	{
		short testval = toHigh(in);
		for(int k = 0; k < findPattern[newType].length; k++)
		{
			if(findPattern[newType][k].contains(testval))
				return k;
		}
		return -1;
	}
	
	private boolean isRoofSign(int xx, int yy, boolean[][] roof)
	{
		if(roof[yy][xx])
			return false;
		if(layout[yy][xx] < 16)  //are you a sign?
		{
			byte count = 4;  //count of roof tiles next to the sign
			if(yy == 0)
				count--;
			else if(roof[yy - 1][xx] == false)
				count--;
			if(yy == 31)
				count--;
			else if(roof[yy + 1][xx] == false)
				count--;
			if(xx == 0)
				count--;
			else if(roof[yy][xx - 1] == false)
				count--;
			if(xx == 31)
				count--;
			else if(roof[yy][xx + 1] == false)
				count--;
			if(count < 2)
				return false;
			return true;
		}
		return false;
	}
	
	private boolean isRoof(int xx, int yy, int lrFound[], int useTileType, boolean useBackup)
	{
		/*if(xx < 0 || xx >= 32)
			return false;
		if(yy < 0 || yy >= 32)
			return false;*/
		
		byte b = layout[yy][xx];
		if(useBackup) 
			b = backupLayout[yy][xx];
		short val = toHigh(b);
		int found = -1;
		for(int k = 9; k <= 21; k++)
		{
			if(findPattern[useTileType][k].contains(val))
			{
				found = k;
				break;
			}
		}
		if(found == -1)
			return false;
		else
		{
			if(found < 17)
			{
				if(replaced[yy][xx])
					return false;
			}
			//left is 0 right is 1
			//if you found a left edge
			if(Arrays.binarySearch(bldgLefts, found) >= 0)
			{
				//if it's greater than right edge return false
				if(xx > lrFound[1] && lrFound[1] != -1)
					return false;
				
			}
			if(xx > lrFound[1] || lrFound[1] == -1)
				lrFound[1] = xx;
			//if you found a right edge
			if(Arrays.binarySearch(bldgRights, found) >= 0)
			{
				//if it's less than left edge return false
				if(xx < lrFound[0] && lrFound[0] != -1)
					return false;
				
			}
			if(xx < lrFound[0] || lrFound[1] == -1)
				lrFound[0] = xx;
			return true;
		}
	}
	
	private boolean isBldg(int xx, int yy, int lrFound[], boolean palace, int tileType)
	{
		if(xx < 0 || xx >= 32)
			return false;
		if(yy < 0 || yy >= 32)
			return false;
		byte b = layout[yy][xx];
		if(b > 0 && b < 80)
		{
			//if(tileType == this.tileType)
				//b = backupLayout[yy][xx];
			return false;
		}
		//else
			//return false;
		short val = toHigh(b);
		int found = -1;
		if(palace)
		{
			for(int k = findPattern[tileType].length - 1;  k >= 0; k--)
			{
				if(findPattern[tileType][k].contains(val))
				{
					found = k;
					break;
				}
			}
		}
		else
		{
			for(int k = 0; k < findPattern[tileType].length; k++)
			{
				if(findPattern[tileType][k].contains(val))
				{
					found = k;
					break;
				}
			}
		}
		if(found == -1)
			return false;
		if(found >= 23 && found <= 25)  //sign post, pottery, and patio
			return false;
		else if(found >= 26)  //palace brick
			return palace;
		else
		{
			if(palace)
				return false;
			//left is 0 right is 1
			//if you found a left edge
			if(Arrays.binarySearch(bldgLefts, found) >= 0)  //attempting to go right
			{
				/*System.out.print("found = " + found + " in lefts; xx = " + xx + " yy = " + yy + " lrFound=" + Arrays.toString(lrFound));
				if(xx > lrFound[1] && lrFound[1] != -1)
					System.out.println("  - Left after Right Blocked");
				else
					System.out.println();*/
				//if it's greater than right edge return false
				if(xx > lrFound[1] && lrFound[1] != -1)
					return false;
			}
			if(xx > lrFound[1] || lrFound[1] == -1)
				lrFound[1] = xx;
			
			//if you found a right edge
			if(Arrays.binarySearch(bldgRights, found) >= 0)  //attempting to go left
			{
				/*System.out.print("found = " + found + " in rights; xx = " + xx + " yy = " + yy + " lrFound=" + Arrays.toString(lrFound));
				if(xx < lrFound[0] && lrFound[0] != -1)
					System.out.println(" - Right after Left Blocked");
				else
					System.out.println();*/
				//if it's less than left edge return false
				if(xx < lrFound[0] && lrFound[0] != -1)
					return false;
			}
			if(xx < lrFound[0] || lrFound[0] == -1)
				lrFound[0] = xx;
			
			//top edge?
			if(Arrays.binarySearch(bldgBots, found) >= 0)  //attempting to go up
			{
				if(yy < lrFound[2] && lrFound[2] != -1)
					return false;
			}
			if(yy < lrFound[2] || lrFound[2] == -1)
				lrFound[2] = yy;
			
			if(Arrays.binarySearch(bldgTops, found) >= 0)  //attempting to go down
			{
				if(yy > lrFound[3] && lrFound[3] != -1)
					return false;
			}
			if(yy > lrFound[3] || lrFound[3] == -1)
				lrFound[3] = yy;
			return true;
		}
	}
	
	private boolean[][] selectRoof(int xx, int yy, int useTileType, boolean useBackup)
	{
		boolean[][] out = new boolean[32][32];
		ArrayList<Point> queue = new ArrayList<Point>();
		byte bb = layout[yy][xx];
		if(useBackup)
			bb = backupLayout[yy][xx];
		int tt = getTileType(bb);
		queue.add(new Point(xx, yy));
		out[yy][xx] = true;
		int last = 0;
		int[] lr = {-1, -1};
		if(Arrays.binarySearch(bldgLefts, tt) >= 0)
			lr[0] = xx;
		if(Arrays.binarySearch(bldgRights, tt) >= 0)
			lr[1] = xx;
		while(last < queue.size())
		{
			Point p = queue.get(last);
			
			if(p.x > 0 && out[p.y][p.x - 1] == false)
			{
				if(isRoof(p.x - 1, p.y, lr, useTileType, useBackup))
				{
					queue.add(new Point(p.x - 1, p.y));
					out[p.y][p.x - 1] = true;
				}
			}
			if(p.x < 31 && out[p.y][p.x + 1] == false)
			{
				if(isRoof(p.x + 1, p.y, lr, useTileType, useBackup))
				{
					queue.add(new Point(p.x + 1, p.y));
					out[p.y][p.x + 1] = true;
				}
			}
			if(p.y > 0 && out[p.y - 1][p.x] == false)
			{
				if(isRoof(p.x, p.y - 1, lr, useTileType, useBackup))
				{
					queue.add(new Point(p.x, p.y - 1));
					out[p.y - 1][p.x] = true;
				}
			}
			if(p.x < 31 && out[p.y + 1][p.x] == false)
			{
				if(isRoof(p.x, p.y + 1, lr, useTileType, useBackup))
				{
					queue.add(new Point(p.x, p.y + 1));
					out[p.y + 1][p.x] = true;
				}
			}
			last++;
		}
		for(int x = lr[0] - 1; x >= 0; x--)
			for(int y = 0; y < 32; y++)
				out[y][x] = false;
		for(int x = lr[1] + 1; x < 32; x++)
			for(int y = 0; y < 32; y++)
				out[y][x] = false;
		return out;
	}
	
	
	private boolean[][] selectBldg(int xx, int yy, int newTileType)  //this is used for testing and doesn't check replaced
	{
		//if(replaced[yy][xx])
			//return null;
		boolean forPalace = false;
		int tt = getTileType(layout[yy][xx], newTileType);
		if(tt < 0)
			return null;
		else if(tt >= 23 && tt <= 25)
			return null;
		else if(tt >= 26)
			forPalace = true;
		boolean[][] out = new boolean[32][32];
		ArrayList<Point> queue = new ArrayList<Point>();
		queue.add(new Point(xx, yy));
		out[yy][xx] = true;
		int last = 0;
		int[] lr = {-1, -1, -1, -1};
		if(Arrays.binarySearch(bldgLefts, tt) >= 0)
			lr[0] = xx;
		if(Arrays.binarySearch(bldgRights, tt) >= 0)
			lr[1] = xx;
		if(Arrays.binarySearch(bldgTops, tt) >= 0)
			lr[2] = yy;
		if(Arrays.binarySearch(bldgBots, tt) >= 0)
			lr[3] = yy;
		while(last < queue.size())
		{
			Point p = queue.get(last);
			if(p.x > 0 && out[p.y][p.x - 1] == false)
			{
				if(isBldg(p.x - 1, p.y, lr, forPalace, newTileType))
				{
					queue.add(new Point(p.x - 1, p.y));
					out[p.y][p.x - 1] = true;
				}
			}
			if(p.x < 31 && out[p.y][p.x + 1] == false)
			{
				if(isBldg(p.x + 1, p.y, lr, forPalace, newTileType))
				{
					queue.add(new Point(p.x + 1, p.y));
					out[p.y][p.x + 1] = true;
				}
			}
			if(p.y > 0 && out[p.y - 1][p.x] == false)
			{
				if(isBldg(p.x, p.y - 1, lr, forPalace, newTileType))
				{
					queue.add(new Point(p.x, p.y - 1));
					out[p.y - 1][p.x] = true;
				}
			}
			if(p.y < 31 && out[p.y + 1][p.x] == false)
			{
				if(isBldg(p.x, p.y + 1, lr, forPalace, newTileType))
				{
					queue.add(new Point(p.x, p.y + 1));
					out[p.y + 1][p.x] = true;
				}
			}
			last++;
		}
		if(!forPalace)
		{
			for(int x = lr[0] - 1; x >= 0; x--)
				for(int y = 0; y < 32; y++)
					out[y][x] = false;
			for(int x = lr[1] + 1; x < 32; x++)
				for(int y = 0; y < 32; y++)
					out[y][x] = false;
		}
		return out;
	}
	
	
	private boolean[][] selectBldg(int xx, int yy)
	{
		if(replaced[yy][xx])
			return null;
		boolean forPalace = false;
		int tt = getTileType(layout[yy][xx]);
		if(tt < 0)
			return null;
		else if(tt >= 23 && tt <= 25)
			return null;
		else if(tt >= 26)
			forPalace = true;
		boolean[][] out = new boolean[32][32];
		ArrayList<Point> queue = new ArrayList<Point>();
		queue.add(new Point(xx, yy));
		out[yy][xx] = true;
		int last = 0;
		int[] lr = {-1, -1, -1, -1};
		//int[] ud = {-1, -1};
		if(Arrays.binarySearch(bldgLefts, tt) >= 0)
			lr[0] = xx;
		if(Arrays.binarySearch(bldgRights, tt) >= 0)
			lr[1] = xx;
		if(Arrays.binarySearch(bldgTops, tt) >= 0)
			lr[2] = yy;
		if(Arrays.binarySearch(bldgBots, tt) >= 0)
			lr[3] = yy;
		while(last < queue.size())
		{
			Point p = queue.get(last);
			
			if(p.x > 0 && out[p.y][p.x - 1] == false && replaced[p.y][p.x - 1] == false)
			{
				if(isBldg(p.x - 1, p.y, lr, forPalace, tileType))
				{
					queue.add(new Point(p.x - 1, p.y));
					out[p.y][p.x - 1] = true;
				}
			}
			if(p.x < 31 && out[p.y][p.x + 1] == false && replaced[p.y][p.x + 1] == false)
			{
				if(isBldg(p.x + 1, p.y, lr, forPalace, tileType))
				{
					queue.add(new Point(p.x + 1, p.y));
					out[p.y][p.x + 1] = true;
				}
			}
			if(p.y > 0 && out[p.y - 1][p.x] == false && replaced[p.y - 1][p.x] == false)
			{
				if(isBldg(p.x, p.y - 1, lr, forPalace, tileType))
				{
					queue.add(new Point(p.x, p.y - 1));
					out[p.y - 1][p.x] = true;
				}
			}
			if(p.y < 31 && out[p.y + 1][p.x] == false && replaced[p.y + 1][p.x] == false)
			{
				if(isBldg(p.x, p.y + 1, lr, forPalace, tileType))
				{
					queue.add(new Point(p.x, p.y + 1));
					out[p.y + 1][p.x] = true;
				}
			}
			last++;
		}
		if(!forPalace)
		{
			for(int x = lr[0] - 1; x >= 0; x--)
				for(int y = 0; y < 32; y++)
					out[y][x] = false;
			for(int x = lr[1] + 1; x < 32; x++)
				for(int y = 0; y < 32; y++)
					out[y][x] = false;
		}
		return out;
	}
	
	private Rectangle getSelectedRectangle(boolean[][] sel)
	{
		int x1 = 99;
		int y1 = 99;
		int x2 = 0;
		int y2 = 0;
		boolean foundBldg = false;
		for(int yy = 0; yy < sel.length; yy++)
		{
			boolean rowEmpty = true;
			for(int xx = 0; xx < sel[0].length; xx++)
			{
				if(sel[yy][xx])
				{
					if(xx < x1)
						x1 = xx;
					if(xx > x2)
						x2 = xx;
					if(yy < y1)
						y1 = yy;
					if(yy > y2)
						y2 = yy;
					rowEmpty = false;
					foundBldg = true;
				}
			}
			if(rowEmpty && foundBldg)
				break;
		}
		Rectangle r = new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
		return r;
	}
	
	public void testPort(int newTileType)
	{
		System.out.println("==== Testing port #" + index + ":" + name + " using TileSet #" + newTileType + " ====");
		ArrayList<Rectangle> bRects = new ArrayList<Rectangle>();
		for(int yy = 0; yy < 32; yy++)
		{
			for(int xx = 0; xx < 32; xx++)
			{
				boolean rectFound = false;
				for(int i = 0; i < bRects.size(); i++)
				{
					if(bRects.get(i).contains(xx, yy))
					{
						rectFound = true;
						break;
					}
				}
				if(rectFound)
					continue;
				int tt = getTileType(layout[yy][xx], newTileType);
				if(tt >= 0 && tt < 26)
				{
					//System.out.println("Found bldg at " + xx + "," + yy);
					Rectangle rr = buildingSpotCheck(xx, yy, newTileType);
					if(rr != null)
						bRects.add(rr);
					//if(xx == 3 && yy == 6)
						//buildingSpotCheck(xx, yy, newTileType);
				}
				else if(tt >= 26)  //ignore palace testing
				{
					boolean[][] bldg = selectBldg(xx, yy, newTileType);
					Rectangle brect = getSelectedRectangle(bldg);
					for(int dy = brect.y + brect.height - 1; dy >= brect.y; dy--)
					{
						boolean found = false;
						for(int dx = brect.x; dx < brect.x + brect.width; dx++)
						{
							if(getTileType(layout[dy][dx], newTileType) == 0)
							{
								Point dp = new Point(dx, dy);
								portDoors.remove(dp);
								found = true;
								break;
							}
						}
						if(found)
							break;
					}
					bRects.add(brect);
				}
			}
		}
		for(int i = 0; i < portDoors.size(); i++)
		{
			System.out.println(" **** ERROR: Failed to find door at " + portDoors.get(i).toString() + ", building may be missing");
			buildingsFailed++;
		}
	}
	
	private Rectangle buildingSpotCheck(int xx, int yy, int newTileType)
	{
		boolean[][] bldg = selectBldg(xx, yy, newTileType);
		if(bldg == null)
			return null;
		boolean failed = false;
		String bldgErr = "  ****  Error in building at " + xx + "," + yy;
		Rectangle brect = getSelectedRectangle(bldg);
		int by1 = brect.y;
		int by2 = brect.y + brect.height;
		int bx1 = brect.x;
		int bx2 = brect.x + brect.width;
		if(by2 < by1 || bx2 < bx1)
			return null;
		if(brect.width == 8)
		{
			selectBldg(xx, yy, newTileType);
		}
		//check 1 - door has not moved
		//System.out.println("Checking building at " + brect.toString());
		for(int dy = by2 - 1; dy >= by1; dy--)
		{
			for(int dx = bx1; dx < bx2; dx++)
			{
				if(getTileType(layout[dy][dx], newTileType) == 0)
				{
					if(getTileType(backupLayout[dy][dx]) != 0)
					{
						System.out.println(bldgErr + " - door has moved");
						failed = true;
					}
					portDoors.remove(new Point(dx, dy));
				}
				if(backupLayout[dy][dx] < 16 && backupLayout[dy][dx] >= 0)
				{
					boolean found = false;
					int signErr = 0;
					if(hasHighFloor[newTileType] != hasHighFloor[tileType])
						signErr = 1;
					for(int i = signErr * -1; i <= signErr * 1; i++)
					{
						if((layout[dy + i][dx] < 16 && layout[dy + i][dx] >= 0))
						{
							found = true;
							break;
						}
					}
					if(!found)
					{
						System.out.println(bldgErr + " - sign has moved; was at " + dx + "," + dy);
						failed = true;
					}
				}		
			}
		}
		//check 2 - holes check
		for(int dy = by1; dy < by2; dy++)
		{
			boolean bldgOn = false;
			boolean rowComplete = false;
			for(int dx = bx1; dx < bx2; dx++)
			{
				if(layout[dy][dx] < 16)  //signs are allowed - do nothing
					continue;
				if(getTileType(layout[dy][dx]) == 22)  //ME scepter pieces - do nothing
					continue;
				if(bldg[dy][dx])
				{
					if(!bldgOn)
					{
						bldgOn = true;
						if(rowComplete)
						{
							System.out.println(bldgErr + " - hole was found in loc " + dx + "," + dy);
							failed = true;
							int[] lr = {-1,-1, -1, -1};
							for(int ddy = by1; ddy < by2; ddy++)
							{
								String s1 = "";
								for(int ddx = bx1; ddx < bx2; ddx++)
								{
									if(bldg[ddy][ddx])
										s1 += ("O ");
									else
										s1 += ("X ");
								}
								String s2 = "  |   ";
								for(int ddx = bx1; ddx < bx2; ddx++)
								{
									if(isBldg(ddx, ddy, lr, false, newTileType))
										s2 += ("O ");
									else
										s2 += ("X ");
								}
								System.out.println(s1 + s2);
							}
						}
					}
				}
				else
				{
					if(bldgOn)
					{
						bldgOn = false;
						rowComplete = true;
					}	
				}
			}
		}
		//check 3 - vertical check
		int[][] vTileTypes = {{9, 11, 14, 15, 16, 20, 21},
				{17, 18, 19, 20, 21},
				{10, 12, 13, 20, 21},
				{ 1,  3,  5,  6},
				{ 2,  4,  7,  8}};
		for(int dx = bx1; dx < bx2; dx++)
		{
			int vstate = -1;  //0 = rooftop, 1 = roof center, 2 = roof bottom, 3 = wall center, 4 = wall bottom
			boolean colError = false;
			boolean signLast = false;
			int[] foundRec = new int[by2 - by1];
			boolean stateChanged = false;
			int tilesCounted = 0;
			for(int dy = by1; dy < by2; dy++)
			{
				if(bldg[dy][dx])
				{
					//int tt = 0;
					//if(layout[dy][dx] >= 16)
					int tt = getTileType(layout[dy][dx], newTileType);
					if(tt == 0)
					{
						if(newTileType == 3 || newTileType == 5)
							vstate = 3;
						continue;
					}
					if(tt == 22)  //ME scepter piece - do nothing
						continue;
					int found = -1;
					if(tt == 20)  //roof special feature
					{
						int ii = findPattern[newTileType][20].indexOf(toHigh(layout[dy][dx]));
						if(newTileType <= 1)
						{
							if(ii == 0)
								found = 0;
							else if(ii == 1)
								found = 2;
							else
								found = -1;
						}
						else if(newTileType == 6)
						{
							if(ii <= 6)
								found = 0;
							else if(ii >= 9 && ii <= 11)
								found = 1;
							else
								found = 2;
						}
						else
							found = -1;
					}
					else if(tt == 21)  //forward roof design
					{
						int ii = findPattern[newTileType][21].indexOf(toHigh(layout[dy][dx]));
						if(newTileType <= 2)
						{
							if(ii == 0)
								found = 0;
							else if(ii <= 6)
								found = 1;
							else
								found = 2;
						}
						else
							found = -1;
					}
					else
					{
						for(int i = 0; i < vTileTypes.length; i++)
						{
							for(int j = 0; j < vTileTypes[i].length; j++)
							{
								if(tt == vTileTypes[i][j])
								{
									found = i;
									break;
								}
							}
							if(found > -1)
								break;
						}
					}
					foundRec[dy - by1] = found;
					tilesCounted++;
					if(found == -1)
					{
						System.out.println(bldgErr + " vertical test not found for tile type " + tt);
						int[] aa = {-1,-1};
						System.out.println("xx=" + dx + " yy=" + dy + " tt=" + tt + " isB=" + isBldg(dx, dy, aa, false, newTileType));
						boolean[][] really = selectBldg(xx, yy, newTileType);
						System.out.println("really[dy][dx]=" + really[dy][dx]);
						colError = true;
					}
					if(!signLast)  //if the last thing found was a sign, vstate could be anywhere
					{
						if(found < vstate)
						{
							System.out.println(bldgErr + " vertical test failure (vstate drop) in col " + dx);
							colError = true;
						}
						if(found > vstate)
						{
							if((vstate == 0 || vstate == 3) && found - vstate > 2)
							{
								System.out.println(bldgErr + " vertical test failure (0 steep jump) in col " + dx);
								colError = true;
							}
							if(vstate > 0 && found - vstate > 1)
							{
								if(!(brect.height == 3 && vstate == 2 && found == 4))
								{
									System.out.println(bldgErr + " vertical test failure (steep jump) in col " + dx);
									colError = true;
								}
							}
							if(found != vstate && vstate != -1)
								stateChanged = true;
							vstate = found;
						}
						
					}
					else
					{
						if(found != vstate && vstate != -1)
							stateChanged = true;
						vstate = found;
						signLast = false;
					}
				}
				else
					signLast = true;
			}
			if(!stateChanged && tilesCounted > 1)
			{
				colError = true;
				System.out.println(bldgErr + " vertical state failed to advance in col " + dx);
			}
			if(colError)
			{
				System.out.print("Column " + dx + " has=");
				for(int dy = by1; dy < by2; dy++)
				{
					int tt = getTileType(layout[dy][dx], newTileType);
					System.out.print(tt + "[" + foundRec[dy - by1] + "]" +",");
				}
				System.out.println();
				failed = true;
			}
		}
		
		//check 4 - horizontal check
		int[][] hTileTypes = {{6, 8},
				{1, 2, 3, 4},
				{5, 7},
				{},
				{9, 10, 17, 20, 21},
				{13, 14, 15, 16, 19, 20, 21},
				{11, 12, 18, 20, 21}};
		for(int dy = by1; dy < by2; dy++)
		{
			int hstate = -16;  //0=left, 1=center, 2=right, 4=roof (OR them)
			boolean roofEnded = false;
			boolean rowError = false;
			int[] foundRec = new int[bx2 - bx1];
			boolean stateChanged = false;
			int tilesCounted = 0;
			for(int dx = bx1; dx < bx2; dx++)
			{
				if(bldg[dy][dx])
				{
					foundRec[dx - bx1] = -2;
					int tt = getTileType(layout[dy][dx], newTileType);
					if(tt == 0 || tt == 22)
						continue;
					if(tt == -1)
					{
						int s1 = getTileType(backupLayout[dy][dx]);
						//int s2 = tt;
						System.out.println("Could not convert tile type " + s1 + " in old type at " + dx + "," + dy);
						continue;
					}
					ArrayList<Short> repl = replacePattern[newTileType][tt];
					if(repl.contains((short) 0))
					{
						int d0 = repl.indexOf((short) 0) + 1;
						short dd = repl.get(d0);
						String adr = allDirs.get(dd);
						if(adr.equals("fwr"))
							tt = 21;
						else if(adr.equals("Irf"))
							tt = 20;
					}
					int found = -1;
					if(tt == 20)  //roof special feature
					{
						int ii = findPattern[newTileType][20].indexOf(toHigh(layout[dy][dx]));
						if(newTileType <= 1)
							found = 5;
						else if(newTileType == 6)
						{
							if(ii == 0 || ii == 7)
								found = 4;
							else if(ii == 6 || ii == 13)
								found = 6;
							else
								found = 5;
						}
						else
							found = -1;
					}
					else if(tt == 21)  //forward roof design
					{
						int ii = findPattern[newTileType][21].indexOf(toHigh(layout[dy][dx]));
						if(newTileType <= 2)
						{
							if(ii == 4 || ii == 7)
							{
								hstate = 4;
								found = 4;
							}
							else if(ii == 6 || ii == 9)
								found = 6;
							else
								found = 5;
						}
						else
							found = -1;
					}
					else
					{
						for(int i = 0; i < hTileTypes.length; i++)
						{
							for(int j = 0; j < hTileTypes[i].length; j++)
							{
								if(tt == hTileTypes[i][j])
								{
									found = i;
									break;
								}
							}
							if(found > -1)
								break;
						}
					}
					tilesCounted++;
					foundRec[dx - bx1] = found;
					if(found == -1)
					{
						System.out.println(bldgErr + " horizontal test not found for tile type " + tt);
						rowError = true;
					}
					if((found & 4) == (hstate & 4))
					{
						int rf = found & 3;
						int hs = hstate & 3;
						if(rf < hs)
						{
							System.out.println(bldgErr + " horizontal test failure (hstate drop) in row " + dy);
							rowError = true;
						}
						if(hstate == -1 && rf > 0)
						{
							System.out.println(bldgErr + " horizontal test failure (-1 test) in row " + dy);
							rowError = true;
						}
						if(hs == 0 && rf > 2)
						{
							System.out.println(bldgErr + " horizontal test failure (0 test) in row " + dy);
							rowError = true;
						}
						if(hs == 1 && rf >= 3)	
						{
							System.out.println(bldgErr + " horizontal test failure (1 test) in row " + dy);
							rowError = true;
						}
					}
					if((hstate & 4) == 4)
					{
						if(found < 4)
						{
							if(roofEnded)
							{
								System.out.println(bldgErr + " horizontal test failure (roofEnded) in row " + dy);
								rowError = true;
							}
							else
								roofEnded = true;
						}
					}
					if(found != hstate)
					{
						if(hstate != -16)
							stateChanged = true;
						hstate = found;
					}
				}
				
			}
			if(!stateChanged && tilesCounted > 1)
			{
				if(isSignOrPost(bx1, dy) && isSignOrPost(bx2 - 1, dy))
					rowError = false;
				else
				{
					rowError = true;
					if(newTileType == 3 && (bx2 - bx1) == 3)
						rowError = false;
					if(rowError)
						System.out.println(bldgErr + " horizontal state failed to advance in row " + dy);
				}
			}
			if(rowError)
			{
				System.out.print("Row " + dy + " has=");
				for(int dx = bx1; dx < bx2; dx++)
				{
					int tt = getTileType(layout[dy][dx], newTileType);
					System.out.print(tt + "[" + foundRec[dx - bx1] + "]" + ",");
				}
				System.out.println();
				failed = true;
			}
		}
		if(failed)
			buildingsFailed++;
		return brect;
	}
	
	private Point hutCheck(int x, int y, int newTileType, Rectangle brect)
	{
		if(hasHighFloor[tileType] && !hasHighFloor[newTileType])
		{
			return raiseBldg(x, y, brect);
		}
		else
			return null;
	}
	
	private Point raiseBldg(int x, int y, Rectangle rr)
	{
		//boolean[][] bb = selectBldg(x, y);
		//Rectangle rr = getSelectedRectangle(bb);
		//find the door
		Point[] doors = getDoors(rr, false);
		if(doors[0].y == rr.y + rr.height - 1)  //door is already on bottom
			return null;
		byte dd = layout[doors[0].y][doors[0].x];
		byte dd2 = 0;
		if(doors[1] != null)
			dd2 = layout[doors[1].y][doors[1].x];
		if(rr.y == 0)  //Timbuktu, Mombassa and Mozambique
		{
			int yy = rr.y + rr.height - 1;  //the bottom of the building (3)
			for(int xx = rr.x; xx <= rr.x + rr.width; xx++)
			{
				layout[yy - 1][xx] = layout[yy][xx]; //row(2) = row(3)
				layout[yy][xx] = layout[yy + 1][xx]; //row(3) = row(4)
			}
			//grassify the sign post
			if(isSignOrPost(rr.x, 4))  //Timbuktu
				layout[4][rr.x] = GRASS;
			else if(isSignOrPost(rr.x + rr.width - 1, 4))  //Mombassa, Mozambique
				layout[4][rr.x + rr.width - 1] = GRASS;
			//replace the door
			layout[doors[0].y][doors[0].x] = dd;
			//bottom wall edges on the buildings
			if(!isSignOrPost(rr.x, 2))
				layout[2][rr.x] = toLow(findPattern[tileType][8].get(0));  //left bottom wall
			if(!isSignOrPost(rr.x + rr.width - 1, 2))
				layout[2][rr.x + rr.width - 1] = toLow(findPattern[tileType][7].get(0));  //right bottom wall
			return new Point(rr.x - 1, rr.y);
		}
		else
		{
			for(int yy = rr.y; yy <= rr.y + rr.height; yy++)  //top -> bottom + 1
			{
				for(int xx = rr.x; xx < rr.x + rr.width; xx++)  //
				{
					layout[yy - 1][xx] = layout[yy][xx];
				}
				if(yy - rr.y >= 2)  //put the wall l/r  (note:pottery is not building)
				{
					int xx = rr.x + rr.width - 1;
					if(yy < rr.y + rr.height - 1)
					{
						if(!isSignOrPost(rr.x, yy))
							layout[yy - 1][rr.x] = toLow(findPattern[tileType][6].get(0));
						if(!isSignOrPost(xx, yy))
							layout[yy - 1][xx] = toLow(findPattern[tileType][5].get(0));
					}
					else if(yy == rr.y + rr.height - 1)
					{
						if(!isSignOrPost(rr.x, yy))
							layout[yy - 1][rr.x] = toLow(findPattern[tileType][8].get(0));
						if(!isSignOrPost(xx, yy))
							layout[yy - 1][xx] = toLow(findPattern[tileType][7].get(0));
					}
				}
				//shadow the +1, but ONLY IF it is a shadow
				int xx = rr.x + rr.width;
				if(xx < 32)
				{
					if(Arrays.binarySearch(shadows, layout[yy][xx]) >= 0)
					{
						layout[yy - 1][xx] = layout[yy][xx];
					}
				}
			}
			layout[doors[0].y][doors[0].x] = dd;
			layout[doors[0].y - 1][doors[0].x] = toLow(findPattern[tileType][1].get(0));
			if(doors[1] != null)
			{
				layout[doors[1].y][doors[1].x] = dd2;
				layout[doors[1].y - 1][doors[1].x] = toLow(findPattern[tileType][1].get(0));
			}
			int xx = rr.x + rr.width;
			if(xx < 32)
			{
				int yy = rr.y + rr.height - 1;
				int ss = Arrays.binarySearch(shadows, layout[yy][xx]);
				if(ss >= 0)
				{
					layout[yy][xx] = (byte) nonShadows[ss];
				}
			}
			return new Point(x - 1, y);
		}	
	}

	private Point trimBuildingAt(int x2, int y2, Rectangle br) 
	{
		//flood select the building @x2,y2
		
		//boolean[][] bldg = selectBldg(x2, y2);
		//find the door
		//Rectangle br = getSelectedRectangle(bldg);
		//System.out.println("Calling trb for bldg at " + br.toString());
		int doorx = -1;
		int doory = -1;
		boolean found = false;
		//for the purposes of this building, we will shrink height and width by 1
		br.height--;
		br.width--;
		for(int yy = br.y; yy <= (br.y + br.height); yy++)
		{
			for(int xx = br.x; xx <= (br.x + br.width); xx++)
			{
				short val = toHigh(layout[yy][xx]);
				if(findPattern[tileType][0].get(0) == val)
				{
					doorx = xx;
					doory = yy;
					found = true;
					break;
				}
			}
			if(found)
				break;
		}
		
		//System.out.println("Trim Building: Door found at " + doorx + "," + doory);
		if(doorx == -1)  ///no idea what we are doing here
			return new Point(x2, y2);
		for(int yy = doory + 1; yy <= br.y + br.height; yy++)  //Venice has a building like this
		{
			//System.out.println("Wiping out row " + yy);
			for(int xx = br.x; xx <= br.x + br.width + 1; xx++)
			{
				layout[yy][xx] = GRASS;
				if(backupLayout[yy][xx] == 28)
					layout[yy][xx] = 28;
			}
		}
		if(doory < br.y + br.height)  
			br.height = doory - br.y;   
		//a door, a wall, and 2 roofs after getting the wingspan
		//System.out.println("Trim building: adjusted rect=" + br.toString());
		for(int yy = 3; yy >= 0; yy--)
		{
			for(int xx = br.x; xx <= (br.x + br.width); xx++)
			{
				replaced[br.y + br.height - yy][xx] = false;
				if(isSignOrPost(xx, br.y + br.height - yy))  //sign
				{
					//System.out.println("Trim Building: Sign found at " + xx + "," + (br.y + br.height - yy));
					continue;
				}
				if(yy == 3)
				{
					if(xx == br.x)
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][9].get(0));  //P roof
					else if(xx == (br.x + br.width))
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][11].get(0));  //7 roof
					else
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][14].get(0));  //roof top
					
				}
				else if(yy == 2)
				{
					if(xx == br.x)
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][10].get(0));  //L roof
					else if(xx == (br.x + br.width))
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][12].get(0));  //J roof
					else
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][13].get(0));  //roof bottom
				}
				else if(yy == 1)
				{
					if(xx == br.x)
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][6].get(0));  //L wall
					else if(xx == (br.x + br.width))
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][5].get(0));  //R wall
					else if(xx != doorx && yy != doory)
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][1].get(0));  //wall
				}
				else
				{
					if(xx == br.x)
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][8].get(0));  //LB wall
					else if(xx == (br.x + br.width))
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][7].get(0));  //RB wall
					else if(xx != doorx && yy != doory)
						layout[br.y + br.height - yy][xx] = toLow(findPattern[tileType][2].get(0));  //b wall
				}
			}
			int xx = br.x + br.width + 1;
			if(xx < 32)
			{
				int f = Arrays.binarySearch(shadows, layout[br.y + br.height - yy][xx]);
				if(f < 0)
					f = Arrays.binarySearch(nonShadows, layout[br.y + br.height - yy][xx]);
				if(yy == 3)   //eliminate shadow
				{
					if(f >= 0)
						layout[br.y + br.height - yy][xx] = (byte) nonShadows[f];
				}
				else if(yy == 0)  //partial shadow
				{
					if(f >= 0)
						layout[br.y + br.height - yy][xx] = (byte) (nonShadows[f] + 1);
				}
				else  //full shadow
				{
					if(f >= 0)
						layout[br.y + br.height - yy][xx] = (byte) (nonShadows[f] + 2);
				}
			}
		}
		for(int yy = br.height - 4; yy >= 0; yy--)
		{
			for(int xx = br.x; xx <= (br.x + br.width); xx++)
			{
				layout[br.y + yy][xx] = GRASS;
				//proc[yy][xx] = true;
			}
			int xx = br.x + br.width + 1;
			if(xx < 32)
			{
				int f = Arrays.binarySearch(shadows, layout[br.y + yy][xx]);
				if(f >= 0)  //eliminate shadow
					layout[br.y + yy][xx] = (byte) nonShadows[f];
			}
		}
		/*int selY = doory - 3;
		System.out.println("Re-selecting building at " + br.x + "," + selY);
		boolean[][] test = selectBldg(br.x, selY);
		Rectangle testR = getSelectedRectangle(test);
		System.out.println("Newly trimmed rectangle =" + testR.toString());*/
		return new Point(br.x - 1, br.y);
	}
	
	private void placeForwardRoof(int x2, int y2, int newType)
	{
		//select the roof
		boolean[][] roof = selectRoof(x2, y2, tileType, true);
		Rectangle rr = getSelectedRectangle(roof);
		int fatstart = 0;
		int fatend = 0;
		int[] heights = new int[32];
		int roofy = rr.y;
		//find the "taller" portions of the roof
		for(int xx = rr.x; xx < rr.x + rr.width; xx++)
		{
			for(int yy = rr.y; yy < rr.y + rr.height; yy++)
			{
				if(replaced[yy][xx])  //skip already placed roof (Venice)
					continue;
				if(roof[yy][xx])
					heights[xx]++;
			}
		}
		int longRoof = 0;
		for(int xx = 0; xx < 32; xx++)
		{
			if(heights[xx] > longRoof && heights[xx] > 2)
			{
				longRoof = heights[xx];
				fatstart = xx;
			}
			else if(heights[xx] == longRoof)
			{
				if(xx - fatstart <= 3)
					fatend = xx;
			}
			else if(heights[xx] == 0)
				break;
		}
		//one building in Istanbul needs to do this
		while(fatend - fatstart >= 3)  //this needs to be 2 or less
		{
			//move fatend to the left
			boolean[][] bldg = buildingSelection;   //this is necessary to get the individual building tiles
			for(int by = 0; by < bldg.length; by++)
			{
				if(bldg[by][fatend] == true)
				{
					//grab the fatend strip and copy it left
					if(bldg[by][fatend - 1])
						layout[by][fatend - 1] = layout[by][fatend];
					//grab the strip to the right of fatend and copy it left
					layout[by][fatend] = layout[by][fatend + 1];
				}
			}
			fatend--;
		}
		int cen = (fatstart + fatend) / 2;
		int n = 0;
		//System.out.println("Placing forward roof at " + cen + "," + roofy);
		layout[roofy][cen] = toLow(replacePattern[newType][21].get(n));
		replaced[roofy][cen] = true;
		n++;
		longRoof--;
		for(int y = 1; y < longRoof; y++)
		{
			for(int x = fatstart; x <= fatend; x++)
			{
				layout[roofy + y][x] = toLow(replacePattern[newType][21].get(n));
				replaced[roofy + y][x] = true;
				n++;
			}
		}
		if(n < 7)  //London
			n = 7;
		int[] lr = {-1,-1};
		for(int x = fatstart; x <= fatend; x++)
		{
			if(isRoof(x, roofy + longRoof, lr, tileType, true))
			{
				layout[roofy + longRoof][x] = toLow(replacePattern[newType][21].get(n));
				replaced[roofy + longRoof][x] = true;
			}
			n++;
		}
	}
	
	private void placeIndianRoof(int x2, int y2, int newType)
	{
		//boolean[][] roof = selectRoof(x2, y2);

		boolean[][] sel = selectRoof(x2, y2, tileType, false);
		Rectangle r = getSelectedRectangle(sel);
		if((r.width & 1) != 1)  //in a rare case (Athens)
		{
			System.out.println("Even width found in Indian roof; using bakup layout");
			sel = selectRoof(x2, y2, tileType, true);  //use the roof from the backup layout
			r = getSelectedRectangle(sel);
			System.out.println("roof rectangle now=" + r.toString());
		}
		//System.out.println("Creating Indian roof for " + r.toString());
		/*String report = "";
		for(int yr = r.y; yr < r.y + r.height; yr++)
		{
			for(int xr = r.x; xr < r.x + r.width; xr++)
			{
				if(sel[yr][xr])
					System.out.print("O");
				else
				{
					System.out.print("X");
					if(layout[yr][xr] > 16)
						report += xr + "," + yr + "=" + getTileType(layout[yr][xr]) + ";";
				}
			}
			System.out.println();
		}
		System.out.println(report);*/
		//place the Indian roof
		
		ArrayList<Short> irf = findPattern[6][20];
		boolean placedRow1 = false;
		boolean placedRow2 = false;
		//boolean venice = false;
		for(int yy = r.y; yy < r.y + r.height; yy++)
		{
			int n = 7;
			int roof = 2;
			boolean venice = false;
			for(int xx = r.x + r.width - 1; xx >= r.x; xx--)
			{
				if(sel[yy][xx])
				{
					//System.out.println("Indian roof processing point " + xx + "," + yy);
					if(xx == 0)
						n = 0;
					else if(sel[yy][xx - 1] || isRoofSign(xx - 1, yy, sel))
						n--;
					else
						n = 0;
					if(r.width == 9 && n == 2 && !venice)   //Venice has a special 9-wide building I'd like to preserve
					{
						n = 4;
						venice = true;
					}
					replaced[yy][xx] = true;
					//the order going r->l is d4c,d4a,d48,d46,d44,d42,d40
					if(!placedRow1)
						layout[yy][xx] = toLow(irf.get(n));
					else if(!placedRow2)
					{
						layout[yy][xx] = toLow(irf.get(n + 7));
						if(sel[yy + 1][xx])  
						{
							if(n != 0 && n != 6)
								layout[yy][xx] = toLow(irf.get(roof + 9));
							roof--;
							if(roof < 0)
								roof = 2;
						}
						else  //substitute the actual bottom of the roof
						{
							if(layout[yy - 1][xx] == 83)
								layout[yy][xx] = 93;
							else if(layout[yy - 1][xx] == 82 || layout[yy - 1][xx] == 84)
								layout[yy][xx] = toLow(irf.get(14));  //dc0 roof bottom
						}
					}
					else
					{
						short rval = toHigh(layout[yy][xx]);
						int rfound = -1;
						for(int k = 0; k < findPattern[tileType].length; k++)
						{
							if(findPattern[tileType][k].contains(rval))
							{
								rfound = k;
								break;
							}
						}
						if(rfound == 21)  //adjust rfound for bottom of forward roof feature
						{
							byte croof = 0;
							if(sel[yy][xx - 1])  //1 means not left edge (right edge possible)
								croof++;
							if(sel[yy][xx + 1])  //2 means not right edge (left edge possible)
								croof += 2;
							short[] frf = {12,10,13};
							rfound = frf[croof];
						}
						short nval = findPattern[newType][rfound].get(0);
						layout[yy][xx] = toLow(nval);
						if(layout[yy][xx] == 98 && layout[yy - 1][xx] == 88)  //copy up the left side (replace L edge with wall)
							layout[yy - 1][xx] = 98;
						else if(layout[yy][xx] == 100 && layout[yy - 1][xx] == 94)  //insert up the right side bridge (replace J edge with wall)
							layout[yy - 1][xx] = 113;   //dc2
						
					}
					if(xx == 0)
					{
						if(placedRow1)
							placedRow2 = true;
						else
							placedRow1 = true;
					}
					else if(sel[yy][xx - 1] == false && !isRoofSign(xx - 1, yy, sel))
					{
						if(placedRow1)
							placedRow2 = true;
						else
							placedRow1 = true;
					}
				}
				else if(isRoofSign(xx, yy, sel))
				{
					//System.out.println("Indian roof processing skipping sign at " + xx + "," + yy);
					n--;
					if(r.width == 9 && n == 2 && !venice)   //Venice has a special 9-wide building I'd like to preserve
					{
						n = 4;
						venice = true;
					}
				}
			}
		}
	
	}
	
}

class POI
{
	int x;
	int y;
	int index;
	String name;
	String text;
}

class GameMapPanel extends JPanel implements MouseListener, MouseMotionListener
{
	GameMap map;
	Color[] colors;
	int xl;
	int yl;
	int left, right, top, bottom;
	int maxr, maxb;
	private int mx;
	private int my;
	public BlockInfoWindow blockInfo;
	
	//private BufferedImage bimg;
	
	ArrayList<Integer> eventData;
	
	GameMapPanel(GameMap mp)
	{
		map = mp;
		Color[] cc = {Color.BLUE, Color.GREEN, new Color(0,128,0), new Color(216,216,0), new Color(64,64,0)};
		colors = cc;
		xl = map.fullMap[0].length;
		yl = map.fullMap.length;
		maxr = xl - 1;
		maxb = yl - 1;
		left = 0;
		right = left + 749;
		top = 0;
		bottom = top + 399;
		eventData = null;
		setPreferredSize(new Dimension(1500,800));
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	GameMapPanel(GameMap mp, ArrayList<Integer> events)
	{
		this(mp);
		eventData = events;
	}
	
	public BufferedImage getMapImage()
	{
		int szx = map.fullMap[0].length * 2;
		int szy = map.fullMap.length * 2;
		BufferedImage bimg = new BufferedImage(szx, szy, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bimg.createGraphics();
		System.out.println("Creating image size " + szx + "x" + szy);
		for(int i = 0; i < map.fullMap.length; i++)
		{
			for(int j = 0; j < map.fullMap[0].length; j++)
			{
				if(map.fullMap[i][j] >= 0 && map.fullMap[i][j] < 5)
					g.setColor(colors[map.fullMap[i][j]]);
				else if(map.fullMap[i][j] >= -68 && map.fullMap[i][j] <= -65)
					g.setColor(Color.WHITE);
				else if(map.fullMap[i][j] >= -64 && map.fullMap[i][j] <= -61)
					g.setColor(Color.YELLOW);
				else if(map.fullMap[i][j] >= -60 && map.fullMap[i][j] <= -57)
					g.setColor(Color.CYAN);
				else
					g.setColor(Color.MAGENTA);
				g.fillRect(j * 2, i * 2, 2, 2);
			}
		}
		if(eventData != null)
		{
			Color[] evColors = new Color[5];
			evColors[0] = new Color(255, 0, 0, 96);
			evColors[1] = new Color(0, 255, 0, 96);
			evColors[2] = new Color(0, 255, 255, 96);
			evColors[3] = new Color(192, 192, 192, 96);
			evColors[4] = new Color(64, 64, 64, 96);
			for(int i = 1; i < eventData.size(); i += 5)
			{
				//if(eventData.get(i) * 2 < (right * 2) && eventData.get(i) * 2 + 144 > (left * 2))  //x1 * 2 < right || x1 * 2 + 144 > left
				//{
					int xx = eventData.get(i) * 2 - (left * 2);
					//int ww = 144;
					//if(eventData.get(i + 2) < (bottom * 2) && eventData.get(i + 2) * 2 + 144 > (top * 2))
					//{
						int yy = eventData.get(i + 2) * 2 - (top * 2);
						int c = eventData.get(i - 1);
						g.setColor(evColors[c - 1]);
						g.fillRect(xx, yy, 144, 144);
						//System.out.println(i + ":ev" + c + " at " + xx + "," + yy + " lft=" + left + " top=" + top);
					//}
				//}
			}
		}
		
		return bimg;
		
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 1500, 800);
		for(int i = top; i <= bottom; i++)
		{
			for(int j = left; j <= right; j++)
			{
				if(map.fullMap[i][j] >= 0 && map.fullMap[i][j] < 5)
					g.setColor(colors[map.fullMap[i][j]]);
				else if(map.fullMap[i][j] >= -68 && map.fullMap[i][j] <= -65)
					g.setColor(Color.WHITE);
				else if(map.fullMap[i][j] >= -64 && map.fullMap[i][j] <= -61)
					g.setColor(Color.YELLOW);
				else if(map.fullMap[i][j] >= -60 && map.fullMap[i][j] <= -57)
					g.setColor(Color.CYAN);
				else
					g.setColor(Color.MAGENTA);
				g.fillRect((j - left) * 2, (i - top) * 2, 2, 2);
			}
		}
		if(eventData != null)
		{
			Color[] evColors = new Color[5];
			evColors[0] = new Color(255, 0, 0, 96);
			evColors[1] = new Color(0, 255, 0, 96);
			evColors[2] = new Color(0, 255, 255, 96);
			evColors[3] = new Color(192, 192, 192, 96);
			evColors[4] = new Color(64, 64, 64, 96);
			for(int i = 1; i < eventData.size(); i += 5)
			{
				if(eventData.get(i) * 2 < (right * 2) && eventData.get(i) * 2 + 144 > (left * 2))  //x1 * 2 < right || x1 * 2 + 144 > left
				{
					int xx = eventData.get(i) * 2 - (left * 2);
					//int ww = 144;
					if(eventData.get(i + 2) < (bottom * 2) && eventData.get(i + 2) * 2 + 144 > (top * 2))
					{
						int yy = eventData.get(i + 2) * 2 - (top * 2);
						int c = eventData.get(i - 1);
						g.setColor(evColors[c - 1]);
						g.fillRect(xx, yy, 144, 144);
						//System.out.println(i + ":ev" + c + " at " + xx + "," + yy + " lft=" + left + " top=" + top);
					}
				}
			}
		}
		if(map.okForPOI != null)
		{
			g.setColor(Color.LIGHT_GRAY);
			for(int xx = 0; xx < map.okForPOI.length; xx++)
			{
				for(int yy = 0; yy < map.okForPOI[xx].length; yy++)
				{
					IndexedList lst = map.okForPOI[xx][yy];
					for(int i = 0; i < lst.size(); i++)
					{
						GameMap.IndexedPoint ip = (GameMap.IndexedPoint) lst.get(i);
						Point p = ip.pt;
						g.fillRect((p.x - left) * 2, (p.y - top) * 2, 2, 2);
					}
				}
			}
		}
		String msdx = "X:" + left + "-" + right;
		String msdy = "Y:" + top + "-" + bottom;
		String xy = "mouse=" + mx + "," + my;
		int ry = my / 2 + top;
		int rx = mx / 2 + left;
		int mp = (int) (map.fullMap[ry][rx] & 255);
		String col = "val[" + ry + "][" + rx + "]=" + mp;
		int mb = (int) (map.backupMap[ry][rx] & 255);
		String col2 = "org[" + ry + "][" + rx + "]=" + mb; 
		int[] dw = map.distToWater(rx, ry, 8);
		int sum = 0;
		for(int i = 0; i < dw.length; i++)
			sum += dw[i];
		String flood = "dtw=" + Arrays.toString(dw) + " sum=" + sum;
		g.setColor(Color.white);
		g.drawString(msdx, 20, 20);
		g.drawString(msdy, 20, 40);
		g.drawString(xy, 20, 60);
		g.drawString(col, 20, 80);
		g.drawString(col2, 20, 100);
		g.drawString(flood, 20, 120);
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) 
	{
		mx = e.getX();
		my = e.getY();
		if(e.getX() < 200 && left > 0)
		{
			left -= 4;
			right -= 4;
			while(left < 0)
			{
				left++;
				right++;
			}
		}
		if(e.getX() > 1300 && right < maxr)
		{
			left += 4;
			right += 4;
			while(right > maxr)
			{
				left--;
				right--;
			}
			
		}
		if(e.getY() < 200 && top > 0)
		{
			top -= 4;
			bottom -= 4;
			while(top < 0)
			{
				top++;
				bottom++;
			}
		}
		if(e.getY() > 600 && bottom < maxb)
		{
			top += 4;
			bottom += 4;
			while(bottom > maxb)
			{
				top--;
				bottom--;
			}
		}
		
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		int ry = my / 2 + top;
		int rx = mx / 2 + left;
		if(blockInfo == null)
		{
			blockInfo = new BlockInfoWindow(map, this);
		}
		blockInfo.bip.setBlock(rx, ry);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
	
}

class GameMapWindow extends JFrame implements ActionListener
{
	GameMapPanel gmp;
	Timer tim;
	
	GameMapWindow(GameMap map)
	{
		gmp = new GameMapPanel(map);
		getContentPane().add(gmp);
		setSize(1520, 820);
		setVisible(true);
		tim = new Timer(100, this);
		tim.start();
	}
	
	GameMapWindow(GameMap map, ArrayList<Integer> events)
	{
		gmp = new GameMapPanel(map, events);
		getContentPane().add(gmp);
		setSize(1520, 820);
		setVisible(true);
		tim = new Timer(100, this);
		tim.start();
	}
	
	public BufferedImage getMapImage()
	{
		return gmp.getMapImage();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		gmp.repaint();
		
	}
}

class BlockInfoPanel extends JPanel
{
	GameMap mapRef;
	int blockX;
	int blockY;
	
	public BlockInfoPanel(GameMap map)
	{
		mapRef = map;
		blockX = 0;
		blockY = 0;
	}
	
	public void setBlock(int rx, int ry)
	{
		blockY = ry / 24;
		blockX = rx / 24;
		repaint();
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.black);
		g.fillRect(0, 0, 200, 200);
		g.setColor(Color.green);
		String out = "";
		int bindex = (blockY * 45) + blockX;
		int delta = mapRef.deltas[blockY][blockX];
		byte[] blk = mapRef.blocks[blockY][blockX];
		out = "Block #" + bindex + "  delta=" + delta;
		int yy = 15;
		g.drawString(out, 5, yy);
		yy += 15;
		out = "";
		for(int y = 0; y < 12; y++)
		{
			for(int x = 0; x < 12; x++)
			{
				String s = Integer.toHexString(blk[y * 12 + x]);
				if(s.length() == 1)
					s = "0" + s;
				out += s + " ";
			}
			g.drawString(out, 5, yy);
			yy += 15;
			out = "";
		}
		
	}
}

class BlockInfoWindow extends JFrame implements WindowListener
{
	BlockInfoPanel bip;
	GameMap map;
	GameMapPanel gmp;
	//Timer tim;
	
	BlockInfoWindow(GameMap map, GameMapPanel gameMapPanel)
	{
		bip = new BlockInfoPanel(map);
		this.map = map;
		getContentPane().add(bip);
		setSize(200, 200);
		addWindowListener(this);
		setVisible(true);
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		gmp.blockInfo = null;
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}

class GameMiniMap
{
	byte[][] fullMap;
	byte[][] backupMap;
	
	ArrayList<Byte[]> allTiles;
	ArrayList<Byte[]> portTiles;
	byte[] tileEntries1;
	ArrayList<Byte> tileEntries2;
	short[][] tileLayout;
	byte[][] portTileLayout;
	private int minimapTilesLoc;
	private int mmPortTilesLoc;
	private int mmLayoutLoc;
	
	private static final int TILE_BASE = 6145;  //0x1801
	
	//minimap output is a0c2->a0fe +  (
	
	GameMiniMap(SNESRom rom)
	{
		/*Compressor comp = new Compressor(rom);
		System.out.println("   - dce796 -");  //tiles
		viewRegion(1894806, comp); //dce796  according to line c0a35a
		System.out.println("============================");
		System.out.println("   - dd0907 -");
		viewRegion(1903367, comp); //dd0907
		System.out.println("============================");
		System.out.println("   - dd05fa -");
		viewRegion(1902586, comp); //dd05fa
		System.out.println("============================");*/
		minimapTilesLoc = -1;
		mmPortTilesLoc = -1;
		allTiles = new ArrayList<Byte[]>();
		portTiles = new ArrayList<Byte[]>();
		tileEntries1 = new byte[260];
		tileEntries2 = new ArrayList<Byte>();
		
	}
	
	public void viewRegion(int reg, Compressor comp)
	{
		
		///int layoutLoc = 1465114;
		ArrayList<Byte> b1 = comp.gameDecompress(reg);
		System.out.println("Layout size=" + b1.size() + " compressed size=" + comp.dataSize + "  data=");
		System.out.print("00:  ");
		for(int i = 0; i < b1.size(); i++)
		{
			String ss = Integer.toHexString(b1.get(i) & 255);
			if(ss.length() < 2)
				ss = "0" + ss;
			System.out.print(ss + " ");
			if((i & 31) == 31)
			{
				System.out.println();
				int xx = i / 32;
				String s2 = String.valueOf(xx);
				if(s2.length() == 1)
					s2 = "0" + s2;
				System.out.print(s2 + ":  ");
			}
		}
	}
	
	private byte determineType(GameMap map, int sx, int sy)
	{
		int water = 0;
		int land = 0;
		for(int yy = 0; yy < 6; yy++)
		{
			for(int xx = 0; xx < 6; xx++)
			{
				if(map.fullMap[sy + yy][sx + xx] == 0)
				{
					water++;
					if(water > 18)
						return 0;
				}
				else
				{
					land++;
					if(land >= 18)
						return 4;
				}
			}
		}
		return 4;
		
	}
	
	public void createMiniMap(GameMap map, boolean viewMiniMap)
	{
		fullMap = new byte[180][360];
		int sx = 0;
		int sy = 0;
		for(int yy = 0; yy < 180; yy++)
		{
			for(int xx = 0; xx < 360; xx++)
			{
				fullMap[yy][xx] = determineType(map, sx, sy);
				sx += 6;
			}
			sx = 0;
			sy += 6;
		}
		System.out.println("Done creating minimap now smoothing minimap");
		smoothMiniMap();
		System.out.println("Finished smoothing minimap");
		if(viewMiniMap)
		{
			MiniMapView mmv = new MiniMapView(this);
		}
		//convertValues();
		//makeLayout();
		//System.out.println("Finished making layout");
	}
	
	private void backupMap()
	{
		backupMap = new byte[fullMap.length][];
		for(int i = 0; i < backupMap.length; i++)
		{
			backupMap[i] = Arrays.copyOf(fullMap[i], fullMap[i].length);
		}
	}
	
	private void smoothMiniMap()
	{
		backupMap();
		ArrayList<Point> qq = new ArrayList<Point>();
		for(int yy = 0; yy < 180; yy++)
		{
			for(int xx = 0; xx < 360; xx++)
			{
				if(backupMap[yy][xx] == 4)
				{
					//up
					byte val = 3;
					if(yy > 0 && backupMap[yy - 1][xx] == 0)
					{
						fullMap[yy][xx] = val;
						qq.add(new Point(xx, yy));
					}
					if(yy < 179 && backupMap[yy + 1][xx] == 0 && qq.size() == 0)
					{
						fullMap[yy][xx] = val;
						qq.add(new Point(xx, yy));
					}
					if(qq.size() == 0)
					{
						int xt = xx - 1;
						if(xt < 0)
							xt = 359;
						if(backupMap[yy][xt] == 0)
						{
							fullMap[yy][xx] = val;
							qq.add(new Point(xx, yy));
						}
						xt = xx + 1;
						if(xt > 359)
							xt = 0;
						if(backupMap[yy][xt] == 0 && qq.size() == 0)
						{
							fullMap[yy][xx] = val;
							qq.add(new Point(xx, yy));
						}
					}
					int end = 0;
					while(end < qq.size())
					{
						Point p = qq.get(end);
						int x = p.x;
						int y = p.y;
						val = fullMap[p.y][p.x];
						if(val == 0)  //skip this point
						{
							end++;
							continue;
						}
						if(y > 0 && fullMap[y - 1][x] < val)
						{
							fullMap[y - 1][x] = (byte) (val - 1);
							qq.add(new Point(x, y - 1));
						}
						if(y < 179 && fullMap[y + 1][x] < val)
						{
							fullMap[y + 1][x] = (byte) (val - 1);
							qq.add(new Point(x, y + 1));
						}
						int xt = x - 1;
						if(xt < 0)
							xt = 359;
						if(fullMap[y][xt] < val)
						{
							fullMap[y][xt] = (byte) (val - 1);
							qq.add(new Point(xt, y));
						}
						xt = x + 1;
						if(xt > 359)
							xt = 0;
						if(fullMap[y][xt] < val)
						{
							fullMap[y][xt] = (byte) (val - 1);
							qq.add(new Point(xt, y));
						}
						end++;
					}
					qq.clear();
				}
			}
		}
	}
	
	private void convertValues()
	{
		byte[] conv = {15,3,4,1,2};
		for(int yy = 0; yy < 180; yy++)
		{
			for(int xx = 0; xx < 360; xx++)
			{
				fullMap[yy][xx] = conv[fullMap[yy][xx]];
			}
		}
	}
	
	//int leftoverTiles;
	int leftoverStart;
	
	class LayoutTiles
	{
		ArrayList<Byte[]> mmTiles1;
		ArrayList<Byte[]> mmTiles2;
		ArrayList<Byte[]> mmTiles3;
		//int[] oldIdx;
		int[] newIdx;
		//ArrayList<Integer> skipMe;
		//byte addingTo;
		
		LayoutTiles()
		{
			mmTiles1 = new ArrayList<Byte[]>();
			mmTiles2 = new ArrayList<Byte[]>();
			mmTiles3 = new ArrayList<Byte[]>();
			//addingTo = 0;
			/*skipMe = new ArrayList<Integer>();
			int v = 8;
			for(int i = 0; i < 28; i++)
			{
				skipMe.add(v);
				v += 2;
			}*/
		}
		
		public ArrayList<Byte[]> process(SNESRom rom)
		{
			int sz = allTiles.size();
			newIdx = new int[sz]; 
			int a = leftoverStart;
			int b = a + 42;
			int e1 = Math.min(sz, a);
			int e2 = Math.min(sz, b);
			//int e3 = sz;
			for(int i = 0; i < e1; i++)  //tiles starting in pos 4000+1 (starts at 0)
			{
				Byte[] tile = allTiles.get(i);
				if(i == 14)
					addEmptyTile(mmTiles1);
				newIdx[i] = mmTiles1.size();
				mmTiles1.add(tile);
				
			}
			for(int i = e1; i < e2; i++)  //tiles starting in pos a740+1 (starts at 825)
			{
				Byte[] tile = allTiles.get(i);
				int j = i - e1;
				if(j >= 8 && j < 36)
				{
					addEmptyTile(mmTiles2);
				}
				newIdx[i] = 825 + mmTiles2.size();
				mmTiles2.add(tile);
			}
			for(int i = e2; i < sz; i++)  //tiles starting in pos b700+1 (starts at 951)
			{
				Byte[] tile = allTiles.get(i);
				int j = i - e2;
				if(j >= 8 && j < 36)
				{
					addEmptyTile(mmTiles3);
				}
				newIdx[i] = 951 + mmTiles3.size();
				mmTiles3.add(tile);
			}
			reprocessLayout();
			reprocessEdge(rom);
			putCopyCode(rom);
			ArrayList<Byte[]> rv = new ArrayList<Byte[]>();
			rv.addAll(mmTiles1);
			rv.addAll(mmTiles2);
			rv.addAll(mmTiles3);
			return rv;
		}
		
		private void reprocessLayout()
		{
			for(int y = 0; y < tileLayout.length; y++)
				for(int x = 0; x < tileLayout[y].length; x++)
					tileLayout[y][x] = (short) newIdx[tileLayout[y][x]]; 
		}
		
		private void reprocessEdge(SNESRom rom)
		{
			int layoutLoc = 736814 - 512;  //cb3c2e
			for(int i = 0; i < 32; i++)
			{
				short val = rom.readShort(layoutLoc);
				val -= TILE_BASE;
				val = (short) newIdx[val];
				val += TILE_BASE;
				rom.writeShort(layoutLoc, val);
				layoutLoc += 2;
			}
			for(int i = 0; i < 23; i++)
			{
				short val = rom.readShort(layoutLoc);
				val -= TILE_BASE;
				val = (short) newIdx[val];
				val += TILE_BASE;
				rom.writeShort(layoutLoc, val);
				layoutLoc += 64;
			}
			for(int i = 0; i < 32; i++)
			{
				short val = rom.readShort(layoutLoc);
				val -= TILE_BASE;
				val = (short) newIdx[val];
				val += TILE_BASE;
				rom.writeShort(layoutLoc, val);
				layoutLoc += 2;
			}
			for(int i = 0; i < 14; i++)
			{
				short val = rom.readShort(layoutLoc);
				val -= TILE_BASE;
				val = (short) newIdx[val];
				val += TILE_BASE;
				rom.writeShort(layoutLoc, val);
				layoutLoc += 2;
			}
			for(int i = 0; i < 23; i++)
			{
				short val = rom.readShort(layoutLoc);
				val -= TILE_BASE;
				val = (short) newIdx[val];
				val += TILE_BASE;
				rom.writeShort(layoutLoc, val);
				layoutLoc += 30;
			}
			for(int i = 0; i < 16; i++)
			{
				short val = rom.readShort(layoutLoc);
				val -= TILE_BASE;
				val = (short) newIdx[val];
				val += TILE_BASE;
				rom.writeShort(layoutLoc, val);
				layoutLoc += 2;
			}
		}
		
		private void putCopyCode(SNESRom rom)
		{
			int tcLoc1 = 344413 - 512;
			rom.writeShort(tcLoc1, mmTiles1.size());
			if(mmTiles2.size() > 0)
			{
				System.err.println(mmTiles2.size() + " a740 tiles are present");
				int codeLoc = SNESRom.CODE_OVERFLOW + rom.codeUsed;
				int callLoc = 344431 - 512;   //c53f6f
				System.out.println("Putting additional minimap tile processing at " + codeLoc + ", mmTiles2 size=" + mmTiles2.size() + ", mmTiles3 size=" + mmTiles3.size());
				String call = "22 " + rom.hexAddr(Integer.toHexString(codeLoc + SNESRom.ROM_BASE), false);
				byte[] bts = rom.strToBytes(call);
				rom.writeBytes(callLoc, bts);
				int v1 = 32768 + (mmTiles1.size() * 32);
				String sv1 = rom.hexAddr(Integer.toHexString(v1), true);
				int v2 = mmTiles2.size();
				String sv2 = rom.hexAddr(Integer.toHexString(v2), true);
				String copy1 = "f4 02 00 f4 7f 00 f4 " + sv1 + " f4 " + sv2 + " f4 3a 03 f4 0b 00 22 00 80 c0 3b 18 69 0c 00 1b";
				bts = rom.strToBytes(copy1);
				rom.writeBytes(codeLoc, bts);
				//rom.allocateCode(bts.length);
				//int alloc = 0;
				int len1 = bts.length;
				if(mmTiles3.size() > 0)
				{
					System.err.println(mmTiles3.size() + " b700 tiles are present");
					v1 = 32768 + ((mmTiles1.size() + mmTiles2.size()) * 32);
					sv1 = rom.hexAddr(Integer.toHexString(v1), true);
					v2 = mmTiles3.size();
					sv2 = rom.hexAddr(Integer.toHexString(v2), true);
					String copy2 = "f4 02 00 f4 7f 00 f4 " + sv1 + " f4 " + sv2 + " f4 b8 03 f4 0b 00 22 00 80 c0 3b 18 69 0c 00 1b " +
							       "af 6a d5 cc 60";
					bts = rom.strToBytes(copy2);
					rom.writeBytes(codeLoc + 32, bts);
					rom.allocateCode(32 + bts.length);
					String hexF = rom.hexAddr(Integer.toHexString(codeLoc + 32), true);
					String copy3 = "20 " + hexF + " 6b";   //the function put at copy2 (overwrites part of copy1)
					bts = rom.strToBytes(copy3);
					rom.writeBytes(codeLoc + len1, bts);
				}
				else
				{
					String copy3 = "af 6a d5 cc 6b";
					bts = rom.strToBytes(copy3);
					rom.writeBytes(codeLoc + len1, bts);
					rom.allocateCode(len1 + bts.length);
				}
			}
			
		}
		
		private void addEmptyTile(ArrayList<Byte[]> list)
		{
			Byte[] empty = new Byte[32];
			for(int i = 0; i < 32; i++)
				empty[i] = 0;
			list.add(empty);
		}
	}
	
	private boolean makeLayout(GameMap map, SNESRom rom)
	{
		tileLayout = new short[23][45];
		portTileLayout = new byte[23][45];
		ArrayList<Integer> cityCounts = new ArrayList<Integer>();
		ArrayList<GameMap.PortData> pda = map.allPortData;
		IndexedList[] portHash = new IndexedList[23];
		for(int i = 0; i < portHash.length; i++)
			portHash[i] = new IndexedList();
		for(int i = 0; i < pda.size(); i++)
		{
			GameMap.PortData pd = pda.get(i);
			if(!pd.isDiscovery)
			{
				portHash[pd.y / 48].add(pd);
				//System.out.println("Added port y=" + pd.y + " to list " + (pd.y / 48));
			}
		}
		int portY = 0;
		for(int yy = 0; yy < 180; yy += 8)
		{
			int portStart = 0;
			int portX = 47;
			GameMap.PortData pdx = (GameMap.PortData) portHash[yy / 8].get(0);
			for(int xx = 0; xx < 360; xx += 8)
			{
				byte[] tile = new byte[32];
				byte[] portTile = new byte[32];
				ArrayList<byte[]> subTiles = new ArrayList<byte[]>();
				//int plottedPorts = 0;
				for(int ty = 0; ty < 8; ty++)
				{
					if(ty + yy >= 180)  //put bottom row
					{
						switch(ty)
						{
						case 4:  
							tile[ty * 2 + 16] = (byte) 255; 
							break;
						case 5:  case 7:  
							tile[ty * 2] = (byte) 255; 
							break;
						case 6:  
							tile[ty * 2 + 1] = (byte) 255;
							break;
						default:  break;
						}
						continue;
					}
					for(int tx = 0; tx < 8; tx++)
					{
						byte vv = fullMap[yy + ty][xx + tx];
						if((vv & 1) > 0)
							tile[ty * 2] |= (1 << (7 - tx));
						if((vv & 2) > 0)
							tile[ty * 2 + 1] |= (1 << (7 - tx));
						if((vv & 4) > 0)
							tile[ty * 2 + 16] |= (1 << (7 - tx));
						if((vv & 8) > 0)
							tile[ty * 2 + 17] |= (1 << (7 - tx));
					}	
				}
				if(pdx != null)
				{
					ArrayList<GameMap.PortData> tilePDs = new ArrayList<GameMap.PortData>();
					int inc = 0;   //maximum binary expansion
					int on = 0;    //"always on" ports
					int lowest = 0;   //the lowest value done
					int plottedPorts = 0;
					while(pdx.x < portX)
					{
						tilePDs.add(pdx);
						inc <<= 1;
						inc++;
						if(!map.knownWorld.contains(pdx.x, pdx.y))
						{
							on |= 1 << plottedPorts;
						}
						portStart++;
						pdx = (GameMap.PortData) portHash[yy / 8].get(portStart);
						plottedPorts++;
						if(pdx == null)
							break;
					}
					if(plottedPorts > 0)
					{
						lowest |= on;  //do this value
						if(lowest == 0)
							lowest = 1;
						subTiles.add(makePortTile(tilePDs, lowest, portX, portY));
						for(int i = 1; i <= inc; i++)
						{
							int val = i | on;
							if(lowest >= val)  //then it's not a unique value
								continue;
							subTiles.add(makePortTile(tilePDs, val, portX, portY));
							lowest = val;
						}
						//now make tile entries
						int tileX = tilePDs.get(0).x / 48;
						int tileY = tilePDs.get(0).y / 48;
						short sxy = (short) (tileY * 32 + tileX); 
						if(tileX >= 32)
						{
							tileX -= 32;
							sxy = (short) (tileY * 13 + tileX + 736);
						}
						byte[] bxy = {(byte) (sxy & 255), (byte) (sxy >> 8)};
						short bti = (short) portTiles.size();
						byte entryIndex = (byte) (tileEntries2.size() / 4);
						tileEntries2.add(bxy[0]);
						tileEntries2.add(bxy[1]);
						byte[] txy = {(byte) (bti & 255), (byte) (bti >> 8)};
						tileEntries2.add(txy[0]);
						tileEntries2.add(txy[1]);
						byte off = 1;
						for(int i = 0; i < plottedPorts; i++)
						{
							GameMap.PortData pd = tilePDs.get(i);
							tileEntries1[pd.origIndex * 2] = entryIndex;
							if((on & (1 << i)) == 0)
							{
								tileEntries1[pd.origIndex * 2 + 1] = off;
								off <<= 1;
							}
							else
								tileEntries1[pd.origIndex * 2 + 1] = 1;
							//off <<= 1;
						}
						
						if(plottedPorts > 1)
						{
							System.out.println(plottedPorts + " cities in same tile; " + subTiles.size() + " tiles made. stats: on=" + on + " inc=" + inc);
						}
					}
				}
				portX += 48;
				
				int found = -1;
				for(int i = 0; i < allTiles.size(); i++)
				{
					boolean match = true;
					Byte[] t2 = allTiles.get(i);
					for(int j = 0; j < 32; j++)
					{
						if(t2[j].byteValue() != tile[j])
						{
							match = false;
							break;
						}
					}
					if(match)
					{
						found = i;
						break;
					}
				}
				if(found == -1)
				{
					Byte[] tx = new Byte[tile.length];
					for(int i = 0; i < tile.length; i++)
						tx[i] = tile[i];
					found = allTiles.size();
					allTiles.add(tx);
				}
				/*if(found >= 14)  //shift 1 for the tile we cannot use - taken care of later
					found++;*/
				tileLayout[yy >> 3][xx >> 3] = (short) found;
				
				//subTile matching can only be done when there is only one city-
				//don't do this if more than one city tile is being made
				cityCounts.add(subTiles.size());
				for(int k = 0; k < subTiles.size(); k++)
				{
					found = -1;
					byte[] pt = subTiles.get(k);
					if(subTiles.size() == 1)
					{
						for(int i = 0; i < portTiles.size(); i++)
						{
							boolean match = true;
							Byte[] t2 = portTiles.get(i);
							for(int j = 0; j < 32; j++)
							{
								if(t2[j].byteValue() != pt[j])
								{
									match = false;
									break;
								}
							}
							if(match)
							{
								found = i;
								int entry = tileEntries2.size() - 2;
								tileEntries2.set(entry, (byte) found);
								cityCounts.remove(cityCounts.size() - 1);
								break;
							}
						}
					}
					if(found == -1)
					{
						Byte[] tx = new Byte[pt.length];
						for(int i = 0; i < pt.length; i++)
							tx[i] = pt[i];
						found = portTiles.size();
						portTiles.add(tx);
					}
				}
			}
			portY += 48;
		}
		System.out.println(allTiles.size() + " minmap tiles out of 490 original");
		
		
		ArrayList<Integer> counts2 = new ArrayList<Integer>();
		for(int i = 0; i < cityCounts.size(); i++)
		{
			boolean found = false;
			for(int j = 0; j < counts2.size(); j += 2)
			{
				if(counts2.get(j) == cityCounts.get(i))
				{
					counts2.set(j + 1, counts2.get(j + 1) + 1);
					found = true;
				}
			}
			if(!found)
			{
				counts2.add(cityCounts.get(i));
			    counts2.add(1);
			}
		}
		for(int i = 0; i < counts2.size(); i += 2)
		{
			System.out.println(counts2.get(i) + ":" + counts2.get(i + 1));
		}
		System.out.println(portTiles.size() + " city tiles out of 192 original");
		
		
		int finalTiles = portTiles.size() + allTiles.size() + 19;
		if(finalTiles > 854)  //768 (tile table for bgs 1 and 2) - 2 (blank and #14) + 88 (add'l tiles in offscreen data)
		{
			System.out.println("Minimap tile count is " + finalTiles + " which exceeded maximum 854 total tiles");
			return false;
		}
		//c53ff8 pushes the number of city tiles to copy
		int ccountLoc = 344569 - 512;
		rom.writeShort(ccountLoc, portTiles.size());
		//we want city tiles to be grouped together so we will put them in the center and divide
		//other minimap tiles
		//we will place them at 0xa020 - size
		int tileCount = portTiles.size();
		int loc = 770 - tileCount;
		System.out.println("City tiles start at #" + loc);
		leftoverStart = loc - 2;
		//c53ffb pushes the offset of tiles for city tiles
		int tcLoc2 = 344572 - 512;
		rom.writeShort(tcLoc2, loc);
		//if(tileCount > 528)
		//{
			int d = loc - 560;
			//c5446a is lda 1e30; we need to adjust this value according to the new start location for city tiles
			int valloc = 345707 - 512;
			short val = rom.readShort(valloc);
			val += d;
			rom.writeShort(valloc, val);
			System.out.println("City tiles were moved ahead " + d + " spaces.");
		//}
			
			/*int tileCount = 528;
			if(allTiles.size() != 490)
			{*/
			//first make sure the minimap is loading the right tiles (offset 1, not offset 32)
			//at c53f60
			rom.data[344416 - 512] = 1;
			//c53f5c pushes the number of minimap tiles onto the stack (step 1)
			tileCount = allTiles.size() + 19;  //need to add the 18 tiles used for the map trimming +1 for the tile that auto-updates
			int tiles1 = loc - 2;
			int tcLoc1 = 344413 - 512;
			rom.writeShort(tcLoc1, tiles1);  //note this is adjusted later
			
			//leftoverTiles = tileCount - tiles1;
			//tileCount -= 32;
			//System.out.println("Too many minimap tiles");
			//return false;
				
			//}
		//tileCount += portTiles.size();
		//if(portTiles.size() != 192)
		//{
			
			//System.out.println("Too many city tiles");
			//return false;
		//}
		
		return true;
	}
	
	private byte[] makePortTile(ArrayList<GameMap.PortData> cityList, int include, int portX, int portY)
	{
		byte[] portTile = new byte[32];
		for(int i = 0; i < cityList.size(); i++)
		{
			if((include & (1 << i)) == 0)
				continue;
			GameMap.PortData pdx = cityList.get(i);
			//plot the port at (x - (portX - 5)) / 6, (y - portY) / 6
			int plotX = (pdx.x - (portX - 47)) / 6;
			int plotY = ((pdx.y - portY) / 6) * 2;
			
			//several bits need to be placed in the tile
			//place the city
			portTile[plotY + 17] |= 1 << (7 - plotX);
			//make sure that only the city is placed there
			byte mask = (byte) (1 << (7 - plotX));
			mask ^= 255;
			portTile[plotY] &= mask;
			portTile[plotY + 1] &= mask;
			portTile[plotY + 16] &= mask;
			//place the city "wings" but only if no other cities are already there
			plotX++;
			if(plotX < 8)
			{
				if((portTile[plotY + 17] & (1 << (7 - plotX))) == 0)
				{
					portTile[plotY] |= 1 << (7 - plotX);
					portTile[plotY + 17] |= 1 << (7 - plotX);
				}
			}
			plotX--;
			plotY += 2;
			if(plotY < 16)
			{
				if((portTile[plotY + 17] & (1 << (7 - plotX))) == 0)
				{
					portTile[plotY + 1] |= 1 << (7 - plotX);
					portTile[plotY + 17] |= 1 << (7 - plotX);
				}
			}
		}
		return portTile;
	}
	
	private void minimapEdge(SNESRom rom, ArrayList<Byte> origTiles, ArrayList<Byte[]> newTiles)
	{
		int[] presList = {0,1,2,3,33,19*16+7,20*16+5,21*16+13,21*16+14,21*16+15,22*16+0,22*16+1,22*16+11,23*16+6,31*16+6,32*16+5,32*16+6,32*16+7};
		int[] newLoc = new int[presList.length];
		boolean[] used = new boolean[presList.length];
		ArrayList<Byte[]> borderTiles = new ArrayList<Byte[]>();
		for(int i = 0; i < presList.length; i++)
		{
			int offset = presList[i] * 32;
			byte[] ot = new byte[32];
			Byte[] bot = new Byte[32];
			for(int j = 0; j < 32; j++)
			{
				ot[j] = origTiles.get(offset + j);
				bot[j] = (Byte) ot[j];
			}
			//newLoc[i] = newTiles.size();
			borderTiles.add(bot);
			used[i] = false;
		}
		int layoutLoc = 736814 - 512;  //cb3c2e
		/*int lld = layoutLoc;
		for(int yy = 0; yy < 25; yy++)
		{
			System.out.print("[" + lld + "]:");
			for(int xx = 0; xx < 64; xx++)
			{
				int val = rom.data[lld] & 255;
				System.out.print(Integer.toHexString(val) + " ");
				lld++;
			}
			System.out.println();
		}
		System.out.println("=====End of block 1======");
		for(int yy = 0; yy < 25; yy++)
		{
			System.out.print("[" + lld + "]:");
			for(int xx = 0; xx < 30; xx++)
			{
				int val = rom.data[lld] & 255;
				System.out.print(Integer.toHexString(val) + " ");
				lld++;
			}
			System.out.println();
		}*/
		//6 map edges to edit
		for(int i = 0; i < 32; i++)
		{
			short val = rom.readShort(layoutLoc);
			val -= 6176;
			int loc = Arrays.binarySearch(presList, val);
			if(!used[loc])
			{
				used[loc] = true;
				newLoc[loc] = newTiles.size();
				newTiles.add(borderTiles.get(loc));
			}
			//System.out.println("x=" + i + " y=" + 0 + " val=" + val + " loc=" + loc);
			val = (short) newLoc[loc];
			val += TILE_BASE;
			rom.writeShort(layoutLoc, val);
			layoutLoc += 2;
		}
		for(int i = 0; i < 23; i++)
		{
			short val = rom.readShort(layoutLoc);
			val -= 6176;
			int loc = Arrays.binarySearch(presList, val);
			if(!used[loc])
			{
				used[loc] = true;
				newLoc[loc] = newTiles.size();
				newTiles.add(borderTiles.get(loc));
			}
			//System.out.println("x=" + 0 + " y=" + i + " val=" + val + " loc=" + loc);
			val = (short) newLoc[loc];
			val += TILE_BASE;
			rom.writeShort(layoutLoc, val);
			layoutLoc += 64;
		}
		for(int i = 0; i < 32; i++)
		{
			short val = rom.readShort(layoutLoc);
			val -= 6176;
			int loc = Arrays.binarySearch(presList, val);
			if(!used[loc])
			{
				used[loc] = true;
				newLoc[loc] = newTiles.size();
				newTiles.add(borderTiles.get(loc));
			}
			//System.out.println("x=" + i + " y=" + 0 + " val=" + val + " loc=" + loc);
			val = (short) newLoc[loc];
			val += TILE_BASE;
			rom.writeShort(layoutLoc, val);
			layoutLoc += 2;
		}
		for(int i = 0; i < 14; i++)
		{
			int val = rom.readShort(layoutLoc) & 65535;
			val -= 6176;
			int loc = Arrays.binarySearch(presList, val);
			if(!used[loc])
			{
				used[loc] = true;
				newLoc[loc] = newTiles.size();
				newTiles.add(borderTiles.get(loc));
			}
			//System.out.println("x=" + i + " y=" + 0 + " val=" + val + " loc=" + loc + "  at " + layoutLoc);
			val = (short) newLoc[loc];
			val += TILE_BASE;
			rom.writeShort(layoutLoc, val);
			layoutLoc += 2;
		}
		for(int i = 0; i < 23; i++)
		{
			int val = rom.readShort(layoutLoc) & 65535;
			val -= 6176;
			int loc = Arrays.binarySearch(presList, val);
			if(!used[loc])
			{
				used[loc] = true;
				newLoc[loc] = newTiles.size();
				newTiles.add(borderTiles.get(loc));
			}
			//System.out.println("x=" + 0 + " y=" + i + " val=" + val + " loc=" + loc);
			val = (short) newLoc[loc];
			val += TILE_BASE;
			rom.writeShort(layoutLoc, val);
			layoutLoc += 30;
		}
		for(int i = 0; i < 16; i++)
		{
			int val = rom.readShort(layoutLoc) & 65535;
			val -= 6176;
			int loc = Arrays.binarySearch(presList, val);
			if(!used[loc])
			{
				used[loc] = true;
				newLoc[loc] = newTiles.size();
				newTiles.add(borderTiles.get(loc));
			}
			//System.out.println("x=" + i + " y=" + 0 + " val=" + val + " loc=" + loc);
			val = (short) newLoc[loc];
			val += TILE_BASE;
			rom.writeShort(layoutLoc, val);
			layoutLoc += 2;
		}
	}
	
	public void testCompressedBytes(SNESRom rom, GameMap map)
	{
		backupMap();
		convertValues();
		
		boolean good = makeLayout(map, rom);
		if(!good)
		{
			rom.romInvalid = 2;  //too many minimap tiles
			return;
		}
		Compressor comp = new Compressor(rom);
		ArrayList<Byte> bts1 = comp.gameDecompress(1894806);  
		rom.mapLocs[2] = 1894806 - 512;
		minimapEdge(rom, bts1, allTiles);
		int osz = comp.dataSize;
		//insertEmptyTiles(allTiles);
		LayoutTiles lt = new LayoutTiles();
		allTiles = lt.process(rom);
		byte[] tileBytes = new byte[allTiles.size() * 32];
		//System.out.println("Pre-compression:");
		for(int i = 0; i < allTiles.size(); i++)
		{
			Byte[] aa = allTiles.get(i);
			for(int j = 0; j < 32; j++)
			{
				tileBytes[i * 32 + j] = aa[j].byteValue();
				//System.out.print(Integer.toHexString(aa[j].byteValue()));
			}
			//System.out.println();
		}
		
		int availSpace = map.getMapFreeSpace();
		if(availSpace == -1)
		{
			comp.compressMap(map);
			availSpace = map.getMapFreeSpace();
		}
		System.out.println("Map Availible space=" + availSpace);
		int endLoc = map.getEndLocation();
		
		
		ArrayList<Byte> bts2 = comp.compressBytes(tileBytes);
		System.out.println(bts2.size() + " bytes used out of " + osz + " bytes available");
		
		int ommUsed = osz;
		
		if(bts2.size() > osz)
		{
			System.out.println("Minimap tiles are TOO LARGE and need to be moved");
			ommUsed = 0;
			if(availSpace + osz > bts2.size())  //put it all on the map
			{
				int lastMapBlock = map.chunks.length - 1;
				while(map.chunks[lastMapBlock].zone != 0)
					lastMapBlock--;
				while(availSpace < bts2.size())
				{
					//move a block of the map to the newly freed area
					int cc = map.chunks[lastMapBlock].chunk.length;
					map.chunks[lastMapBlock].move(0, 2);
					availSpace += cc;
					endLoc -= cc;
					lastMapBlock--;
					ommUsed += cc;
				}
				minimapTilesLoc = endLoc;
				availSpace -= bts2.size();
				endLoc += bts2.size();
			}
			else if(bts2.size() < (rom.DATA_MAX - rom.dataUsed))  //put it all in overflow
			{
				minimapTilesLoc = rom.DATA_OVERFLOW + rom.dataUsed;
				rom.allocateData(bts2.size());
			}
			else
			{
				System.out.println("Insufficient space for minimap tiles (needed=" + bts2.size() + " avail=" + availSpace);
				rom.romInvalid = 3;
				return;
			}
			int delta = minimapTilesLoc - SNESRom.MAP_LOC;
			int loadLoc = 344354 - 512;  //loading from end of map; change initial load loc to map start
			byte[] newLoc = rom.strToBytes("6e cc c3");
			rom.writeBytes(loadLoc, newLoc);
			loadLoc += 6;
			newLoc[0] += 2;
			rom.writeBytes(loadLoc, newLoc);
			
			int deltaLoc = 652051 - 512;  //c9f113
			rom.write3Bytes(deltaLoc, delta);
			rom.writeBytes(minimapTilesLoc, bts2);
			System.out.println("Successfully moved minimap tiles to " + minimapTilesLoc);
			//ommAvail = osz;
			
		}
		else
		{
			minimapTilesLoc = 1894806 - 512;
			rom.writeBytes(minimapTilesLoc, bts2);
		}
		
		
		//city tiles
		bts1 = comp.gameDecompress(1902586);
		rom.mapLocs[3] = 1902586 - 512;
		osz = comp.dataSize;
		byte[] cityTileBytes = new byte[portTiles.size() * 32];
		for(int i = 0; i < portTiles.size(); i++)
		{
			Byte[] aa = portTiles.get(i);
			for(int j = 0; j < 32; j++)
				cityTileBytes[i * 32 + j] = aa[j].byteValue();
		}
		ArrayList<Byte> bts3 = comp.compressBytes(cityTileBytes);
		System.out.println(bts3.size() + " bytes used out of " + osz + " bytes available");
		if(bts3.size() > osz)
		{
			System.out.println("City tiles are TOO LARGE and need to be moved");
			if(bts3.size() < (osz - ommUsed))  //put in old minimap tile spot
			{
				mmPortTilesLoc = 1894806 - 512 + ommUsed;
				System.out.println("Moving city tiles to old minimap tiles location");
				/*int loadLoc = 344510 - 512;  //change initial load loc to 
				String hex = Integer.toHexString(mmPortTilesLoc + SNESRom.ROM_BASE);
				String addr = rom.hexAddr(hex, false);
				byte[] newLoc = rom.strToBytes(addr);
				rom.writeBytes(loadLoc, newLoc);*/
				int dd = 155902 + ommUsed;  //orig delta + used distance
				String ds = rom.hexAddr(Integer.toHexString(dd), false);  //put the delta to be the delta for the minimap tile's original location
				byte[] delta = rom.strToBytes(ds);
				int deltaLoc = 652054 - 512;  //c9f116
				rom.writeBytes(deltaLoc, delta);
				
				rom.writeBytes(mmPortTilesLoc, bts3);
				System.out.println("Successfully moved port tiles to " + mmPortTilesLoc);
			}
			else 
			{
				if(bts3.size() < availSpace + osz)  //put at end of map
				{
					int lastMapBlock = map.chunks.length - 1;
					while(map.chunks[lastMapBlock].zone != 0)
						lastMapBlock--;
					while(availSpace < bts3.size())
					{
						//move a block of the map to the newly freed area
						int cc = map.chunks[lastMapBlock].chunk.length;
						map.chunks[lastMapBlock].move(0, 3);
						availSpace += cc;
						endLoc -= cc;
						lastMapBlock--;
					}
					System.out.println("Putting city tiles at end of map");
					mmPortTilesLoc = endLoc;
				}
				else if(bts3.size() < (rom.DATA_MAX - rom.dataUsed))  //put it all in overflow
				{
					mmPortTilesLoc = rom.DATA_OVERFLOW + rom.dataUsed;
					System.out.println("Putting city tiles in overflow");
					rom.allocateData(bts3.size());
				}
				else
				{
					System.out.println("Insufficient space for minimap city tiles");
					rom.romInvalid = 3;
					return;
				}
				int delta = mmPortTilesLoc - SNESRom.MAP_LOC;
				int loadLoc = 344510 - 512;  //loading from end of map; change initial load loc to map start
				byte[] newLoc = rom.strToBytes("6e cc c3");
				rom.writeBytes(loadLoc, newLoc);
				loadLoc += 6;
				newLoc[0] += 2;
				rom.writeBytes(loadLoc, newLoc);
				
				int deltaLoc = 652054 - 512;  //c9f116
				rom.write3Bytes(deltaLoc, delta);
				rom.writeBytes(mmPortTilesLoc, bts3);
				System.out.println("Successfully moved city tiles to " + mmPortTilesLoc);
			} 
		}
		else
		{
			mmPortTilesLoc = 1902586 - 512;
			rom.writeBytes(mmPortTilesLoc, bts3);
		}
		dumpLayout(rom);
	}
	
	/*private void insertEmptyTiles(ArrayList<Byte[]> mmTiles) //taken care of by LayoutTiles class
	{
		Byte[] empty = new Byte[32];
		mmTiles.add(14, empty);
		if(leftoverTiles > 0)
		{
			
		}
	}*/

	public void dumpLayout(SNESRom rom)
	{
		int layoutLoc = 736814 - 512 + 66;  //cb3c2e
		System.out.println("Starting layout output at " + layoutLoc);
		for(int yy = 0; yy < tileLayout.length; yy++)
		{
			//first row is map top
			for(int xx = 0; xx < 31; xx++)
			{
				rom.writeShort(layoutLoc, tileLayout[yy][xx] + TILE_BASE);
				layoutLoc += 2;
			}
			layoutLoc += 2;
		}
		layoutLoc += 62;
		//System.out.println("Continuing layout output at " + layoutLoc);
		layoutLoc = 737932;
		for(int yy = 0; yy < tileLayout.length; yy++)
		{
			//first row is map top
			for(int xx = 0; xx < 14; xx++)
			{
				rom.writeShort(layoutLoc, tileLayout[yy][xx + 31] + TILE_BASE);
				layoutLoc += 2;
			}
			layoutLoc += 2;
		}
		int portLayout = 740979 - 512;  //cb4c73;
		rom.writeBytes(portLayout, tileEntries1);  //size is always 260
		int portLayout2 = 741239 - 512;  //cb4d77  - need to stop clobbering data if data goes beyond 741626 (size > 387)
		if(tileEntries2.size() > 387)  //need to move it somewhere else
		{
			System.out.println("City minimap entries exceeded maximum of 387");
			rom.romInvalid = 4;
			return;
		}
		rom.writeBytes(portLayout2, tileEntries2);
	}
	
}

class MiniMapPan extends JPanel
{
	GameMiniMap mm;
	Color[] cols;
	
	MiniMapPan(GameMiniMap mp)
	{
		mm = mp;
		cols = new Color[5];  //water, water edge, center, land edge, land
		int[] rs = {100,200,50,150,250};
		int[] gs = { 75,150,37,112,187};
		int[] bs = { 25, 50,12, 37, 62};
		/*int[] rs = {0,0,127,0,0};
		int[] gs = {0,0,127,195,255};
		int[] bs = {127,255,127,0,0};*/
		for(int i = 0; i < 5; i++)
			cols[i] = new Color(rs[i], gs[i], bs[i]);
	}
	
	public void paintComponent(Graphics g)
	{
		int dx = 0;
		int dy = 0;
		for(int yy = 0; yy < mm.backupMap.length; yy++)
		{
			for(int xx = 0; xx < mm.backupMap[0].length; xx++)
			{
				g.setColor(cols[mm.backupMap[yy][xx]]);
				g.fillRect(dx, dy, 2, 2);
				dx += 2;
			}
			dy += 2;
			dx = 0;
		}
	}
}

class MiniMapView extends JFrame
{
	MiniMapPan mmp;
	
	MiniMapView(GameMiniMap mm)
	{
		mmp = new MiniMapPan(mm);
		//this.map = map;
		getContentPane().add(mmp);
		setSize(800, 500);
		//addWindowListener(this);
		setTitle(UWNHRando.outputRomName);
		setVisible(true);
	}
	
}

class GameMap
{
	byte[][] fullMap;
	byte[][] backupMap;
	byte[][][] eventMap;
	
	POI[] pois;
	Port[] ports;
	
	ArrayList<PortData> allPortData;
	
	IndexedList[][] okForPOI;
	
	byte[][][] blocks;
	int[][] deltas;
	
	int compressedSize;
	
	Rectangle knownWorld;
	
	GameMap()
	{
		fullMap = new byte[1080][];
		for(int i = 0; i < fullMap.length; i++)
			fullMap[i] = new byte[2160];
		
		eventMap = new byte[4][][];
		for(int i = 0; i < eventMap.length; i++)
		{
			eventMap[i] = new byte[15][];
			for(int j = 0; j < eventMap[i].length; j++)
				eventMap[i][j] = new byte[30];
		}
		compressedSize = -1;
		knownWorld = null;
		chunks = null;
	}
	
	

	public void editKnownWorld(SNESRom rom) 
	{
		int[] locs = {478171, 478176, 478196, 478201};
		if(knownWorld.x < 0)
			knownWorld.x = 0;
		if(knownWorld.y < 0)
			knownWorld.y = 0;
		System.out.println("KW:" + knownWorld.toString());
		int[] vals = {knownWorld.y, knownWorld.y + knownWorld.height, knownWorld.x, knownWorld.x + knownWorld.width};
		int[] vv = new int[4];
		for(int i = 0; i < 4; i++)
		{
			locs[i] -= 512;
			if(i < 2)
				vv[i] = (int) Math.floor(vals[i] / 24);
			else
				vv[i] = (int) Math.ceil(vals[i] / 24);
			rom.data[locs[i]] = (byte) vv[i];
			
		}
		if(rom.noKWStorms)  //then eliminate events in the Known World
		{
			int ev = 998532 - 512;  //f3c84
			int y1 = (int) Math.floor(vals[0] / 72);
			int y2 = (int) Math.ceil(vals[1] / 72);
			int x1 = (int) Math.floor(vals[2] / 72);
			int x2 = (int) Math.ceil(vals[3] / 72);
			ev += ((y1 * 30) + x1);
			for(int yy = y1; yy <= y2; yy++)
			{
				for(int xx = x1; xx <= x2; xx++)
				{
					rom.data[ev] &= 63;  //turns off events here
					ev++;
				}
				ev += 30;
			}
		}
	}



	public void reorderPortData() //puts capitals in their proper order
	{
		int[] caps = {0,1,2,8,29,33};
		ArrayList<PortData> capitols = new ArrayList<PortData>();
		for(int i = 0; i < allPortData.size(); i++)
		{
			if(allPortData.get(i).isCapital)
			{
				capitols.add(allPortData.remove(i));
				i--;
			}
		}
		for(int i = 0; i < caps.length; i++)
			allPortData.add(caps[i], capitols.get(i));
		allPortData.get(77).isCapital = true;
		for(int i = 0; i < allPortData.size(); i++)
			allPortData.get(i).setOrigIndex(i);
	}

	GameMap(byte[][] map)
	{
		this();
		fullMap = map;
		backup();
	}
	
	private void testBackup()
	{
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(backupMap[y][x] != fullMap[y][x])
					System.out.println("backup failure: difference in tile " + x + "," + y + " map=" + fullMap[y][x] + " backup=" + backupMap[y][x]);
			}
		}
	}
	
	public int getMapFreeSpace()
	{
		if(compressedSize > 0)
			return 77313 - compressedSize;
		return -1;
	}
	
	public int getEndLocation() 
	{
		// TODO Auto-generated method stub
		return 1163902 + compressedSize + 1;
	}
	
	public void backup() 
	{
		backupMap = new byte[fullMap.length][];
		for(int i = 0; i < backupMap.length; i++)
		{
			backupMap[i] = Arrays.copyOf(fullMap[i], fullMap[i].length);
		}
		//testBackup();
	}
	
	public void restoreBackup()
	{
		fullMap = new byte[backupMap.length][];
		for(int i = 0; i < fullMap.length; i++)
		{
			fullMap[i] = Arrays.copyOf(backupMap[i], backupMap[i].length);
		}
		//testBackup();
	}
	
	public boolean compareTo(GameMap gmo)
	{
		boolean same = true;
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(fullMap[y][x] != gmo.fullMap[y][x])
				{
					System.out.println("Difference in map at " + x + "," + y + "; orig=" + fullMap[y][x] + "  new=" + gmo.fullMap[y][x]);
					same = false;
				}
			}
		}
		return same;
	}
	
	public void listCounts()
	{
		int[] counts = new int[256];
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				int idx = fullMap[y][x] & 255;
				counts[idx]++;
			}
		}
		for(int i = 0; i < counts.length; i++)
		{
			if(counts[i] > 0)
				System.out.println(i + ":" + counts[i]);
		}
	}
	
	public void simplifyInvisible()   //simplifies invisible land (a 32x32 area is visible on the screen at each water point)
	{
		boolean[][] vis = new boolean[fullMap.length][];  
		for(int i = 0; i < vis.length; i++)
			vis[i] = new boolean[fullMap[0].length];  
		//water should all already be accessible
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(fullMap[y][x] == 0)
				{
					int minY = Math.max(y - 16, 0);
					int maxY = Math.min(y + 16, 1079);
					int minX = x - 16;
					int maxX = x + 16;
					for(int yy = minY; yy <= maxY; yy++)
					{
						for(int xx = minX; xx <= maxX; xx++)
						{
							int ex = xx;
							if(ex < 0)
								ex += 2160;
							if(ex >= 2160)
								ex -= 2160;
							vis[yy][ex] = true;
						}
					}
				}
			}
		}
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(vis[y][x] == false)
					fullMap[y][x] = 1;
			}
		}
	}
	
	

	public void cliffify() //turns edge water maps into cliffs
	{
		byte[][] out = new byte[fullMap.length][fullMap[0].length];
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(fullMap[y][x] == 0)
				{
					//byte out = 0;
					int[] dw = distToLand(x, y, (byte) 0);
					for(int i = 0; i < 4; i++)  //up,down,left,right
						if(dw[i] == 1)
							out[y][x] |= (8 << i);
					//fullMap[y][x] = out;
				}
			}
		}
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				fullMap[y][x] |= out[y][x];
			}
		}
	}
	
	private byte[] makeMountain(byte in)
	{
		byte[] rv = {in, (byte) (in+1), (byte) (in+2), (byte) (in+3)};
		return rv;
	}
	
	private byte[] makeTree(byte in)
	{
		byte[] rv = {in, 0};
		if(in == -88 || in == -85)
			rv[1] = (byte) (in + 2);
		else
			rv[1] = (byte) (in + 1);
		return rv;
	}

	public void zonify() 
	{
		int[] ys = {24,192,360,480,720,840,960,1056,1080};
		byte[][] smap = new byte[ys.length][];  //contains plains, forest, desert, mountains
		byte[] v1 = {0,(byte) 128,(byte) 128,(byte) 128,(byte) 128};  //arctic
		smap[0] = v1;
		byte[] v2 = {0, (byte) 128, (byte) 168, (byte) 134, (byte) 144};  //tundra
		smap[1] = v2;
		byte[] v3 = {0, (byte) 128, (byte) 171, (byte) 134, (byte) 150};  //temperate
		smap[2] = v3;
		byte[] v4 = {0, (byte) 128, (byte) 175, (byte) 134, (byte) 156};  //subtropical
		smap[3] = v4;
		byte[] v5 = {0, (byte) 128, (byte) 178, (byte) 134, (byte) 162};  //tropical
		smap[4] = v5;
		smap[5] = v4;
		smap[6] = v3;
		smap[7] = v2;
		smap[8] = v1;
		
		byte[][] cmap = new byte[ys.length][];  //u,d,l,r,ul,ur,bl,bd -,_,=,I-,-I,7,P,J,L,II
		int[] valsTM = {1,2,3,4,8,5,9,6,10,12};
		byte[] ca = {20,17,21,24,18,19,25,22,28,26};  //arctic
		
		/*byte[] ct = {36,33,37,40,34,35,};  //tundra
		byte[] cm = {};  //temperate
		byte[] cs = {};  //subtropical
		byte[] cr = {};  //tropical*/
		int mtnCount = 0;
		for(int i = 0; i < cmap.length; i++)
		{
			cmap[i] = new byte[16];
			for(int j = 0; j < valsTM.length; j++)
			{
				cmap[i][valsTM[j]] = ca[j];
				if(i < 5)
					cmap[i][valsTM[j]] += 16 * i;
				else
					cmap[i][valsTM[j]] += 16 * (8 - i);
			}
		}
		//desert cliff
		byte[] dc = new byte[16];
		for(int j = 0; j < valsTM.length; j++)
			dc[valsTM[j]] = (byte) (ca[j] + 80);
		int vzone = 0;
		//int last = 0;
		//listCounts();
		for(int y = 0; y < fullMap.length; y++)
		{
			while(ys[vzone] - y <= 0)
				vzone++;
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(fullMap[y][x] >= 1)
				{
					if(fullMap[y][x] < 5)
					{
						//last = fullMap[y][x];
						//if(fullMap[y][x] == 4)
							//mtnCount++;
						if(fullMap[y][x] == 4)
						{
							byte[] mtn = makeMountain(smap[vzone][fullMap[y][x]]);
							fullMap[y][x] = mtn[0];
							fullMap[y + 1][x] = mtn[2];
							fullMap[y][x + 1] = mtn[1];
							fullMap[y + 1][x + 1] = mtn[3];
						}
						else if(fullMap[y][x] == 2)
						{
							byte[] tree = makeTree(smap[vzone][fullMap[y][x]]);
							fullMap[y][x] = tree[0];
							fullMap[y + 1][x] = tree[1];
						}
						else
							fullMap[y][x] = smap[vzone][fullMap[y][x]];
					}
					/*else
					{
						//cliff processing
						ArrayList<Point> pts = getSurr4(new Point(x, y), fullMap[0].length - 1, fullMap.length - 1, true);
						int des = 0;
						int there = 0;
						if(pts.size() == 4)
							there = 15;
						else if(pts.size() == 2)
							there = 12;
						else if(pts.get(0).y < y)
							there = 13;
						else
							there = 14;
						int lookAt = 1;
						for(int p = 0; p < pts.size(); p++)
						{
							Point pp = pts.get(p);
							while((there & lookAt) == 0)
								lookAt <<= 1;
							byte test = fullMap[pp.y][pp.x];
							if(test == 3 || test == -122)  //desert
								des |= lookAt;
						}
						if(fullMap[y][x] >> 3 == des)
							fullMap[y][x] = dc[des];
						else
							fullMap[y][x] = cmap[vzone][fullMap[y][x] >> 3];
					}*/
				}
			}
		}
		//listCounts();
	}
	
	public void desertify()  //processes desert edges
	{
		//old l=184, lu=182, ld=187, r=183, ru=180 rd=185
		int[] dedge = {134, 181, 186, 134, 183, 180, 185, 134, 184, 182, 187, 134, 134, 134, 134, 134};  //n,u,d,ud,l,lu,ld,lud,r,ru,rd,rud,rl,rlu,rld,rlud
		byte[][] out = new byte[fullMap.length][fullMap[0].length];
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(fullMap[y][x] == -122)
				{
					//byte out = 0;
					int[] dw = distToLand(x, y, (byte) 134);
					for(int i = 0; i < 4; i++)  //up,down,left,right
						if(dw[i] == 1)
							out[y][x] |= (1 << i);
					//fullMap[y][x] = (byte) dedge[out];
				}
			}
		}
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(out[y][x] != 0)
					fullMap[y][x] = (byte) dedge[out[y][x]];
			}
		}
		
	}

	public void decompressOrigMap(SNESRom rom)
	{
		int mapBaseLoc = 1163902; //11c72e; the 512 header is shaved off during decompression
		//rom.outputData(mapBaseLoc, 50);
		int deltaLoc = 249786 - 512;  //3cfba
		//rom.outputData(deltaLoc, 50);
		Compressor cc = new Compressor(rom);
		//cc.gameDecompress(mapBaseLoc + 3519);
		int hiDelta = 0;
		int[] tileCounts = new int[256];
		int[] tileCounts2 = new int[2];
		int[] blockcounts = new int[256];
		int hiVal = 0;
		blocks = new byte[45][90][];
		deltas = new int[45][90];
		for(int yy = 0; yy < 45; yy++)
		{
			for(int xx = 0; xx < 90; xx++)
			{
				/*if(xx > 89)
				{
					deltaLoc += 3;
					continue;
				}*/
				int delta = 0;
				for(int i = 0; i < 3; i++)
				{
					int d = (int) (rom.data[deltaLoc + i] & 255);
					delta += (d << (8 * i));
				}
				if(delta > hiDelta)
					hiDelta = delta;
				//System.out.println("delta at " + xx + "," + yy + "=" + delta);
				ArrayList<Byte> bts = cc.gameDecompress(mapBaseLoc + delta);
				byte[] barr = new byte[bts.size()];
				//System.out.print("Decompressed bytes:");
				for(int i = 0; i < barr.length; i++)
				{
					barr[i] = bts.get(i);
					//System.out.print(barr[i] + ",");
				}
				blocks[yy][xx] = barr;
				deltas[yy][xx] = delta;
				if(xx == 44 && yy == 1)
				{
					barr = new byte[bts.size()];
					//System.out.print("Decompressed bytes:");
					for(int i = 0; i < barr.length; i++)
					{
						barr[i] = bts.get(i);
						System.out.print(barr[i] + ",");
					}
					//System.out.println();
					int ds1 = cc.dataSize;
					int ds0 = mapBaseLoc + delta;
					//for(int i = 0; i < ds1; i++)
						//System.out.print(UWNHRando.viewByte(rom.data[ds0 - 512 + i], cc.controlMarking, i) + ",");
					cc.gameDecompress(mapBaseLoc + delta);
					
				}
				int ii = 0;
				for(int y = 0; y < 24; y += 2)
				{
					for(int x = 0; x < 24; x += 2)
					{
						int val = (int) (bts.get(ii) & 255);
						ii++;
						int yo = yy * 24 + y;
						int xo = xx * 24 + x;
						if(val < 16)
						{
							if((val & 8) > 0)
								fullMap[yo][xo] = -128;
							else
								fullMap[yo][xo] = 0;
							if((val & 2) > 0)
								fullMap[yo + 1][xo] = -128;
							else
								fullMap[yo + 1][xo] = 0;
							xo++;
							if((val & 4) > 0)
								fullMap[yo][xo] = -128;
							else
								fullMap[yo][xo] = 0;
							if((val & 1) > 0)
								fullMap[yo + 1][xo] = -128;
							else
								fullMap[yo + 1][xo] = 0;
							int bc = Long.bitCount(val);
							tileCounts2[1] += bc;
							tileCounts2[0] += (4 - bc);
						}
						else
						{
							val <<= 2;
							if(val > hiVal)
								hiVal = val;
							int tile = 983552 + val - 512; //f0200
							int[] look = new int[4];//rom.data[tile];
							for(int i = 0; i < 4; i++)
								look[i] = (rom.data[tile + i] & 255);
							fullMap[yo][xo] = rom.data[tile];
							fullMap[yo + 1][xo] = rom.data[tile + 2];
							fullMap[yo][xo + 1] = rom.data[tile + 1];
							fullMap[yo + 1][xo + 1] = rom.data[tile + 3];
							for(int i = 0; i < 4; i++)
								tileCounts[(int) (rom.data[tile + i] & 255)]++;
							blockcounts[val / 4]++;
						}
					}
				}
				deltaLoc += 3;
			}
		}
		//System.out.println();
		/*for(int i = 0; i < 255; i++)
			System.out.print(rom.data[983552 - 512 + i] + ",");
		System.out.println();*/
		//for(int i = 0; i < tileCounts.length; i++)
			//System.out.println(i + ":" + tileCounts[i]);
		//System.out.println("From val < 16");
		//for(int i = 0; i < 2; i++)
			//System.out.println(i + ":" + tileCounts2[i]);
		//System.out.println("HiDelta = " + hiDelta);
		ArrayList<Byte> bts = cc.gameDecompress(mapBaseLoc + hiDelta);
		int finalsize = hiDelta + cc.dataSize;
		/*rom.outputData(mapBaseLoc + hiDelta - 512, 50);
		System.out.println("Total map size =" + finalsize);
		System.out.println("HiVal = " + hiVal);
		System.out.println("Blockcounts:");
		int tile = 983552 - 512;
		for(int i = 0; i < blockcounts.length; i++)
			System.out.println(i + ":" + blockcounts[i] + " " + Arrays.toString(Arrays.copyOfRange(rom.data, tile + 4 * i, tile + 4 * i + 4)));
		*/
		backup();
		adjOrigTilemap();
	}
	
	public void adjOrigTilemap() 
	{
		ArrayList<Byte> grasses = new ArrayList<Byte>();
		int[] ggs = {128,129,130,131,132,133,21,26,37,42,53,58,69,74,39,55};
		for(int i = 0; i < ggs.length; i++)
			grasses.add((byte) ggs[i]);
		ArrayList<Byte> trees = new ArrayList<Byte>();
		int[] tts = {168,170,171,173,175,176,178,179};
		for(int i = 0; i < tts.length; i++)
			trees.add((byte) tts[i]);
		ArrayList<Byte> deserts = new ArrayList<Byte>();
		int[] des = {134,181,184,183,186,187,182,185,180};
		for(int i = 0; i < des.length; i++)
			deserts.add((byte) des[i]);
		ArrayList<Byte> mountains = new ArrayList<Byte>();
		int[] mts = {144,145,146,147,150,151,152,153,156,157,158,159,162,163,164,165};
		for(int i = 0; i < mts.length; i++)
			mountains.add((byte) mts[i]);
		ArrayList<Byte> special = new ArrayList<Byte>();
		int[] spcl = {228,229,230,231,232,233,204,205,206,207,192,193,194,195,196,197,198,199,188,189,190,191};
		for(int i = 0; i < spcl.length; i++)
			special.add((byte) spcl[i]);
		int[] reg = {0,0,0,0,0,0};
		String[] regT = {"Water", "Grass", "Trees", "Desert", "Mountains", "Special"};
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				byte b = fullMap[y][x];
				if(grasses.contains(b))
					fullMap[y][x] = 1;
				else if(trees.contains(b))
					fullMap[y][x] = 2;
				else if(deserts.contains(b))
					fullMap[y][x] = 3;
				else if(mountains.contains(b))
					fullMap[y][x] = 4;
				else if(special.contains(b) == false)
				//else
					fullMap[y][x] = 0;
				if(fullMap[y][x] >= 0 && fullMap[y][x] <= 4)
					reg[fullMap[y][x]]++;
				else
					reg[5]++;
				/*int ss = (int) (b & 255);
				if(ss >= 228 && ss <= 233)
					System.out.println("Special:" + ss + " at " + x + "," + y);*/
			}
		}
		for(int i = 0; i < reg.length; i++)
			System.out.println(regT[i] + " count=" + reg[i]);
	}

	private boolean pointInList(Point pc, ArrayList<Point> pts)  //do not do this for floodfill
	{
		for(int i = pts.size() - 1; i >= 0; i--)
		{
			if(pc.equals(pts.get(i)))
				return true;
		}
		return false;
	}
	
	private ArrayList<Point> getSurr4(Point src, int xMax, int yMax, boolean wrapX)
	{
		ArrayList<Point> rv = new ArrayList<Point>(4);
		if(src.y > 0)
			rv.add(new Point(src.x, src.y - 1));
		if(src.y < yMax)
			rv.add(new Point(src.x, src.y + 1));
		if(src.x > 0)
			rv.add(new Point(src.x - 1, src.y));
		else if(wrapX)
			rv.add(new Point(xMax, src.y));
		if(src.x < xMax)
			rv.add(new Point(src.x + 1, src.y));
		else if(wrapX)
			rv.add(new Point(0, src.y));
		return rv;
	}
	
	private ArrayList<Point> getSurr8(Point src, int xMax, int yMax, boolean wrapX)
	{
		ArrayList<Point> rv = new ArrayList<Point>(8);
		if(src.y > 0)
		{
			rv.add(new Point(src.x, src.y - 1));
			if(src.x < xMax)
				rv.add(new Point(src.x + 1, src.y - 1));
			else if(wrapX)
				rv.add(new Point(0, src.y - 1));
		}
		if(src.y < yMax)
		{
			rv.add(new Point(src.x, src.y + 1));
			if(src.x < xMax)
				rv.add(new Point(src.x + 1, src.y + 1));
			else if(wrapX)
				rv.add(new Point(0, src.y + 1));
		}
		if(src.x > 0)
		{
			rv.add(new Point(src.x - 1, src.y));
			rv.add(new Point(src.x - 1, src.y + 1));
		}
		else if(wrapX)
		{
			rv.add(new Point(xMax, src.y));
			rv.add(new Point(xMax, src.y + 1));
		}
		if(src.x < xMax)
		{
			rv.add(new Point(src.x + 1, src.y));
			rv.add(new Point(src.x + 1, src.y + 1));
		}
		else if(wrapX)
		{
			rv.add(new Point(0, src.y));
			rv.add(new Point(0, src.y + 1));
		}
		return rv;
	}
	
	private ArrayList<Point> growPoint(Point src, byte[][] arr, byte val, double cGrowX, double cGrowY)
	{
		//boolean rv = false;
		ArrayList<Point> pts = getSurr4(src, 2157, 1057, true);
		for(int i = 0; i < pts.size(); i++)
		{
			Point p = pts.get(i);
			if(arr[p.y][p.x] != val)
			{
				if(p.x != src.x)
				{
					if(UWNHRando.rand() < cGrowX || cGrowX >= 1.0)
					{
						arr[p.y][p.x] = val;
					}
					else
					{
						pts.remove(i);
						i--;
					}
				}
				else
				{
					if(UWNHRando.rand() < cGrowY || cGrowY >= 1.0)
					{
						arr[p.y][p.x] = val;
					}
					else
					{
						pts.remove(i);
						i--;
					}
				}
			}
			else
			{
				pts.remove(i);
				i--;
			}
		}
		return pts;
	}
	
	private ArrayList<Point> floodPoint(Point src, byte[][] arr, byte val, int minSizeX, int minSizeY, double drop, double minFlow)
	{
		ArrayList<Point> rv = new ArrayList<Point>();
		rv.add(src);
		int expanded = 0;
		double chanceX = 1.0;
		double chanceY = 1.0;
		double ratioX = 1.0;
		double ratioY = 1.0;
		double dropX = drop;
		double dropY = drop;
		int minsqr = 0;
		//int[] negCount = {0,0,0,0,0};
		//int[] negEnd = {-10, -30, -50, -80, -150};
		if(minSizeX > minSizeY)
		{
			ratioX = minSizeY / (minSizeX * 1.0);
			//dropX *= ratioX;
			minsqr = minSizeY * minSizeY;
		}
		else
		{
			ratioY = minSizeX / (minSizeY * 1.0);
			//dropY *= ratioY;
			minsqr = minSizeX * minSizeX;
		}
		/*int mnsqx = minSizeX * minSizeX;
		int mnsqy = minSizeY * minSizeY;*/
		while(expanded < rv.size())
		{
			Point p = rv.get(expanded);
			double dx = (p.x - src.x) * ratioX;
			//dx *= dx;
			double dy = (p.y - src.y) * ratioY;
			//dy *= dy;
			int dd = (int) (minsqr - ((dx * dx) + (dy * dy)));
			chanceX = 1.0;
			//dx = minsqr - dx;
			if(dd < 0)
			{
				chanceX += dd * dropX;
				if(chanceX < minFlow)
					chanceX = minFlow;
				/*for(int i = 0; i < negCount.length; i++)
				{
					if(dd > negEnd[i])
					{
						negCount[i]++;
						break;
					}
				}*/
			}
			//if(chanceX > 0.98)
				//chanceX = 0.96;
			chanceY = chanceX;
			/*dy = minsqr - dy;
			if(dy < 0)
			{
				chanceY += dy * dropY;
				if(chanceY < 0.15)
					chanceY = 0.15;
			}*/
			rv.addAll(growPoint(p, arr, val, chanceX, chanceY));
			expanded++;
		}
		/*String ncs = "Neg counts:";
		for(int i = 0; i < negCount.length; i++)
			ncs += negCount[i] + ",";
		System.out.println(ncs);*/
		return rv;
	}
	
	private byte makeContinent(Rectangle r, boolean forceSea)
	{
		int minSX = r.width >> 6;
		int minSY = r.height >> 6;
		int cx = (r.width >> 1) + r.x;
		int cy = (r.height >> 1) + r.y;
		fullMap[cy][cx] = 1;
		Point s = new Point(cx, cy);
		
		double drop = 1.0 / Math.min(r.width, r.height);
		floodPoint(s, fullMap, (byte) 1, minSX, minSY, drop * 0.01, 0.49);
		
		if(UWNHRando.rand() > 0.5 || forceSea)  //lake within
		{
			int lw = (int) (UWNHRando.rand() * 20);
			lw = (int) (((lw + 20.0) / 100.0) * r.width);
			int lh = (int) (UWNHRando.rand() * 20);
			lh = (int) (((lh + 20.0) / 100.0) * r.height);
			int lx = (int) (UWNHRando.rand() * 25);
			lx = (int) (((lx + 30.0) / 100) * r.width + r.x);
			int ly = (int) (UWNHRando.rand() * 25);
			ly = (int) (((ly + 30.0) / 100.0) * r.height + r.y);
			Rectangle lr = new Rectangle(lx, ly, lw, lh);
			minSX = lr.width >> 1;
			minSY = lr.height >> 1;
			cx = minSX + lr.x;
			cy = minSY + lr.y;
			fullMap[cy][cx] = 0;
			s = new Point(cx, cy);
			
			drop = 1.0 / Math.min(minSX, minSY);
			floodPoint(s, fullMap, (byte) 0, minSX, minSY, drop, 0.49);
			
			if(UWNHRando.rand() > 0.7)  //island within
			{
				int iw = (int) (UWNHRando.rand() * 20);
				iw = (int) (((iw + 20.0) / 100.0) * lr.width);
				int ih = (int) (UWNHRando.rand() * 20);
				ih = (int) (((ih + 20.0) / 100.0) * lr.height);
				int ix = (int) (UWNHRando.rand() * 25);
				ix = (int) (((ix + 30.0) / 100) * lr.width + lr.x);
				int iy = (int) (UWNHRando.rand() * 25);
				iy = (int) (((iy + 30.0) / 100.0) * lr.height + lr.y);
				Rectangle ir = new Rectangle(ix, iy, iw, ih);
				minSX = ir.width >> 1;
				minSY = ir.height >> 1;
				cx = minSX + ir.x;
				cy = minSY + ir.y;
				fullMap[cy][cx] = 1;
				s = new Point(cx, cy);
				
				drop = 1.0 / Math.min(minSX, minSY);
				floodPoint(s, fullMap, (byte) 1, minSX, minSY, drop, 0.45);
			}
			return 0;
		}
		return 1;
	}
	
	private void makeIsland(Rectangle r)
	{
		int minSX = r.width >> 2;
		int minSY = r.height >> 2;
		int cx = (r.width >> 1) + r.x;
		int cy = (r.height >> 1) + r.y;
		fullMap[cy][cx] = 1;
		Point s = new Point(cx, cy);
		int ss = Math.min(r.width, r.height);
		double drop = 1.0 / ss;
		int[] mins = {40, 80, 150, 225};
		for(int i = 0; i < mins.length; i++)
		{
			if(ss > mins[i])
			{
				minSX >>= 1;
				minSY >>= 1;
				drop *= 0.5;
			}
		}
		floodPoint(s, fullMap, (byte) 1, minSX, minSY, drop, 0.3);
		
		if(UWNHRando.rand() > 0.5 && ss >= 80)  //lake within
		{
			int lw = (int) (UWNHRando.rand() * 20);
			lw = (int) (((lw + 20.0) / 100.0) * r.width);
			int lh = (int) (UWNHRando.rand() * 20);
			lh = (int) (((lh + 20.0) / 100.0) * r.height);
			int lx = (int) (UWNHRando.rand() * 25);
			lx = (int) (((lx + 30.0) / 100) * r.width + r.x);
			int ly = (int) (UWNHRando.rand() * 25);
			ly = (int) (((ly + 30.0) / 100.0) * r.height + r.y);
			Rectangle lr = new Rectangle(lx, ly, lw, lh);
			minSX = lr.width >> 1;
			minSY = lr.height >> 1;
			cx = minSX + lr.x;
			cy = minSY + lr.y;
			fullMap[cy][cx] = 0;
			s = new Point(cx, cy);
			
			drop = 1.0 / Math.min(minSX, minSY);
			floodPoint(s, fullMap, (byte) 0, minSX, minSY, drop, 0.49);
		}
		
	}
	
	private void makeArchipelago(Rectangle r)
	{
		
		//cut one side in half
		if(UWNHRando.rand() > 0.5)
			r.width /= 2;
		else
			r.height /= 2;
		for(int i = 0; i < 3; i++)
		{
			int fails = 0;
			ArrayList<Rectangle> arcrs = new ArrayList<Rectangle>();
			while(fails < 50)
			{
				int rw = (int) (UWNHRando.rand() * 60) + 15;
				int rh = (int) (UWNHRando.rand() * 60) + 15;
				int rx = (int) (UWNHRando.rand() * (r.width - rw)) + r.x;
				int ry = (int) (UWNHRando.rand() * (r.height - rh)) + r.y;
				Rectangle ir = new Rectangle(rx, ry, rw, rh);
				if(testAgainstList(ir, arcrs))
				{
					makeIsland(ir);
					arcrs.add(ir);
				}
				else
					fails++;
			}
		}
	}
	
	private void makeRiver(int x, int y, int dir)
	{
		
	}
	
	private int rectOlap(Rectangle r1, Rectangle r2)
	{
		int rv = 0;
		if(r1.contains(r2.x, r2.y))
			rv |= 1;
		if(r1.contains(r2.x + r2.width, r2.y))
			rv |= 2;
		if(r1.contains(r2.x, r2.y + r2.height))
			rv |= 4;
		if(r1.contains(r2.x + r2.width, r2.y + r2.height))
			rv |= 8;
		if(r2.contains(r1.x, r1.y))
			rv |= 16;
		if(r2.contains(r1.x + r1.width, r1.y))
			rv |= 32;
		if(r2.contains(r1.x, r1.y + r1.height))
			rv |= 64;
		if(r2.contains(r1.x + r1.width, r1.y + r1.height))
			rv |= 128;
		return rv;
	}
	
	private boolean testAgainstList(Rectangle r, ArrayList<Rectangle> rects)
	{
		for(int i = 0; i < rects.size(); i++)
		{
			Rectangle rt = rects.get(i);
			if(rectOlap(rt, r) > 0)
				return false;
		}
		return true;
	}
	
	private Rectangle growRect(Rectangle r, ArrayList<Rectangle> rects, int maxX, int maxY)
	{
		boolean[] dirs = {true, true, true, true};
		while(r.width < 300 && r.height < 300)
		{
			for(int i = 0; i < 4; i++)
			{
				if(dirs[i] == false)
					continue;
				switch(i)
				{
				case 0:  
					r.width += 5; 
					if(r.x + r.width > maxX) {r.width -= 5; dirs[0] = false;}
				break;
				case 1:  
					r.height += 5; 
					if(r.y + r.height > maxY) {r.height -= 5; dirs[1] = false;}
					break;
				case 2:  
					r.x -= 5; r.width += 5;
					if(r.x < 0) {r.x += 5;  r.width -= 5; dirs[2] = false;}
					break;
				case 3:  r.y -= 5; r.height += 5;  
					if(r.y < 0) {r.y += 5; r.height -= 5; dirs[3] = false;}
					break;
				}
				if(testAgainstList(r, rects) == false)
				{
					switch(i)
					{
					case 0:  r.width -= 5; break;
					case 1:  r.height -= 5; break;
					case 2:  r.x += 5; r.width -= 5; break;
					case 3:  r.y += 5; r.height -= 5;  break;
					}
					dirs[i] = false;
				}
			}
			boolean keepGoing = false;
			for(int i = 0; i < 4; i++)
				if(dirs[i])
					keepGoing = true;
			if(!keepGoing)
				break;
		}
		return r;
	}
	
	private boolean checkWaterBox(int x, int y, boolean up)
	{
		byte val = 0;
		byte count = 0;
		int sx = 0;
		if(up)
		{
			for(int yy = -1; yy <= 0; yy++)
			{
				for(int xx = -1; xx <= 1; xx++)
				{
					sx = x + xx;
					if(sx < 0)
						sx = fullMap[0].length - 1;
					if(sx == fullMap[0].length)
						sx = 0;
					if(fullMap[y + yy][sx] == 0)
					{
						val |= (1 << count);
					}
					count++;
				}
			}
		}
		else
		{
			for(int xx = -1; xx <= 0; xx++)
			{
				for(int yy = -1; yy <= 1; yy++)
				{
					sx = x + xx;
					if(sx < 0)
						sx = fullMap[0].length - 1;
					if(sx == fullMap[0].length)
						sx = 0;
					if(fullMap[y + yy][sx] == 0)
					{
						val |= (1 << count);
					}
					count++;
				}
			}
		}
		if((val & 15) == 15)  //001111
			return true;
		if((val & 60) == 60)  //111100
			return true;
		return false;
	}
	
	private ArrayList<Point> placePorts(int nPorts, int regionX, int regionY, ArrayList<Point> allPois)
	{
		/*int xx = regionX * 80;
		int yy = regionY * 80 + 20;
		ArrayList<Point> possPorts = new ArrayList<Point>();
		for(int y = 0; y < 80; y += 2)
		{
			for(int x = 0; x < 80; x += 2)
			{
				//gather the portable places on the map
				if((x + xx < 0) || (y + yy < 0))
					System.out.println("RegionX is " + regionX + "  and RegionY is " + regionY);
				int[] dd = distToWater(xx + x, yy + y, 3);  //up,dn,lf,rgt
				for(int i = 0; i < 4; i++)
				{
					boolean ok = false;
					if((i & 1) == 0)
					{
						if(dd[i] == 1)  //up or left
						{
							if(i == 0)  //up
								ok = checkWaterBox(x + xx, y + yy - 1, true);
							else
								ok = checkWaterBox(x + xx - 1, y + yy, false);
							if(ok)
							{
								possPorts.add(new Point(x + xx, y + yy));
								break;
							}
						}
					}
					else
					{
						if(dd[i] == 2)  //down or right
						{
							if(i == 1)  //down
								ok = checkWaterBox(x + xx, y + yy + 3, true);
							else
								ok = checkWaterBox(x + xx + 3, y + yy, false);
							if(ok)
							{
								possPorts.add(new Point(x + xx, y + yy));
								break;
							}
						}
					}
				}
			}
		}*/
		if(okForPOI[regionX][regionY].size() == 0)
		{
			return null;
		}
		
		ArrayList<Point>  currPois = new ArrayList<Point>();
		boolean placedPOI = true;
		//Point fpt = null;
		for(int i = 0; i < nPorts; i++)
		{
			int fail = 0;
			while(fail < 1000)
			{
				if(okForPOI[regionX][regionY].size() == 0)
				{
					return null;
				}
				int pp = (int) (UWNHRando.rand() * okForPOI[regionX][regionY].size());
				IndexedPoint ipt = (GameMap.IndexedPoint) okForPOI[regionX][regionY].get(pp);
				//boolean placed = true;
				Point fpt = new Point(ipt.pt.x & 4094, ipt.pt.y & 4094);
				boolean okToPlace = true;
				for(int y = 0; y < 1; y++)
				{
					for(int x = 0; x < 1; x++)
					{
						if(fullMap[fpt.y + y][fpt.x + x] == 0)
						{
							okForPOI[regionX][regionY].remove(pp);
							okToPlace = false;
						}
					}	
				}
				if(!okToPlace)
				{
					fail++;
					continue;
				}
				/*if(currPois.contains(fpt))  //these checks might not be necessary
				{
					//placed = false;
					fail++;
					continue;
				}
				if(allPois.contains(fpt))
				{
					//placed = false;
					fail++;
					continue;
				}*/
				//int rx = regionX;
				//int ry = regionY;
				//Point[] prad = new Point[50];  //exclusion radius
				//int ll = 0;
				int removed = 0;
				for(int y = -4; y <= 4; y++)
				{
					for(int x = -4; x <= 4; x++)
					{
						//if(x == 0 && y == 0)
							//continue;
						
						int px = fpt.x + x;
						if(px < 0)
							px += 2160;
						if(px >= 2160)
							px -= 2160;
						int regx = px / 80;
						int py = fpt.y + y;  //the 20 pixel buffer prevents y wrapping
						if(py < 20 || py >= 1060)
							continue;
						int regy = (py - 20) / 80;
						Point ep = new Point(px, py);
						//prad[ll] = ep;
						IndexedPoint ip = new IndexedPoint(ep);
						
						if(okForPOI[regx][regy].removeItemWithIndex(ip.getIndex()) != null)
							removed++;
						//ll++;
					}
				}
				currPois.add(fpt);
				//System.err.println("Placing POI at " + fpt.toString() + " removed " + removed + " possible POI locations");
				//possPorts.remove(fpt);
				/*for(int l = 0; l < prad.length; l++)
				{
					currPois.add(prad[l]);
					possPorts.remove(prad[l]);
				}*/
				fail = 0;
				break;
			}
			if(fail >= 1000)
			{
				placedPOI = false;
				return null;
			}
		}
		if(placedPOI)
		{
			//if(currPois.size() == 0)
				//System.out.println();
			return currPois;
		}
		else
			return null;
	}
	
	class PortData implements Indexed
	{
		int palette;
		int possCultures;
		int origCulture;
		int finalCulture;
		boolean isCapital;
		boolean isSupply;
		boolean isDiscovery;
		int[] startingSupport;
		byte iaData;
		int origIndex;
		int x;
		int y;
		int openDirs;
		
		PortData(int pc, boolean cap, int[] ssup)
		{
			possCultures = pc;
			isCapital = cap;
			startingSupport = ssup;
			iaData = 6;
			isSupply = false;
			isDiscovery = false;
			openDirs = 0;
			origCulture = -1;
		}
		
		public int selectPalette(int orig) 
		{
			if(origCulture == -1)
				return orig;
			ArrayList<Integer> possOut = new ArrayList<Integer>();
			switch(finalCulture)
			{
			case 0:  case 1:
				int[] p0 = {0,1,2};
				for(int i = 0; i < p0.length; i++)
					possOut.add(p0[i]);
				break;
			case 2:
				possOut.add(3);
				if(y < 192 || y > 960)
					possOut.add(0);
				break;
			case 3:  case 5:
				possOut.add(finalCulture + 1);
				if(origCulture == 5)
					possOut.add(5);
				break;
			case 4:
				possOut.add(finalCulture + 1);
				if(origIndex != 2 && origIndex != 77)
					if(y < 480 || y > 720)
						possOut.add(8);
				break;
			case 6:
				possOut.add(3);
				possOut.add(7);
				break;
			case 7:
				possOut.add(8);
				if(origCulture == 5)
					possOut.add(5);
				break;
			case 8:
				return 9;
			}
			int r = (int) (UWNHRando.rand() * possOut.size());
			return possOut.get(r);
		}

		public void setSupplyOnly(boolean supply)
		{
			isSupply = supply;
		}
		
		public void setDiscovery(boolean id)
		{
			isDiscovery = id;
		}
		
		public void setXY(int xx, int yy)
		{
			x = xx;
			y = yy;
		}
		
		public void setIAData(byte ia)
		{
			iaData = ia;
		}
		
		public void setOrigIndex(int idx)
		{
			origIndex = idx;
		}
		
		public void setOrigCulture(int idx)
		{
			origCulture = idx;
		}
		
		public void setOpenings(GameMap gm)
		{
			/*******
			 *  76
			 * 0PP5
			 * 1PP4
			 *  23
			 ********/
			int[] a = gm.distToWater(x, y, 3);  //up down left right
			//7 2 0 5
			if(a[0] == 1)
				openDirs |= (1 << 7);
			if(a[1] == 2)
				openDirs |= (1 << 2);
			if(a[2] == 1)
				openDirs |= (1 << 0);
			if(a[3] == 2)
				openDirs |= (1 << 5);
			
			a = gm.distToWater(x + 1, y + 1, 3);
			//6 3 1 4
			if(a[0] == 2)
				openDirs |= (1 << 6);
			if(a[1] == 1)
				openDirs |= (1 << 3);
			if(a[2] == 2)
				openDirs |= (1 << 1);
			if(a[3] == 1)
				openDirs |= (1 << 4);
		}

		@Override
		public long getIndex() 
		{
			// TODO Auto-generated method stub
			return (x << 16) + y;
		}
	}
	
	private int[] selectRegion(int[] prevRegion, int[] popProbs, int[][] population, boolean kwrestrict)
	{
		int regx = 0;
		int regy = 0;
		if(prevRegion == null)
		{
			int r = (int) (UWNHRando.rand() * 72);
			regy = 0;
			for(int i = 0; i < popProbs.length; i++)
			{
				r -= popProbs[i];
				if(r <= 0)
					break;
				regy++;
			}
			if(kwrestrict)
				regx = (int) (UWNHRando.rand() * 17) + 2;
			else
				regx = (int) (UWNHRando.rand() * 27);
		}
		else
		{
			regx = prevRegion[0];
			regy = prevRegion[1];
			do
			{
				if(UWNHRando.rand() > 0.5)  //change X only
				{
					if(UWNHRando.rand() > 0.5)
						regx++;
					else
						regx--;
					if(kwrestrict)
					{
						if(regx < 0)
							regx = 1;
						else if(regx >= 19)
							regx = 18;
					}
					else
					{
						if(regx < 0)
							regx = 26;
						else if(regx >= 27)
							regx = 0;
					}
				}
				else  //change Y only
				{
					if(regy == 0)
						regy = 1;
					else if(regy == popProbs.length - 1)
						regy--;
					else
					{
						int totalprob = popProbs[regy - 1] + popProbs[regy + 1];
						int r = (int) (UWNHRando.rand() * totalprob);
						if(r < popProbs[regy - 1])
							regy--;
						else
							regy++;
					}
				}
			}while(population[regx][regy] >= 10);
		}
		int[] rv = {regx, regy};
		return rv;
	}
	
	public void addPOIs()
	{
		//divide the map into 27x13 80x80 regions, excluding the top 20 and bottom 20 tiles
		int[][] population  = new int[27][13];
		ArrayList<Point> allPois = new ArrayList<Point>();
		ArrayList<PortData> pdata = new ArrayList<PortData>();
		int placedPorts = 0;
		//byte placedSupply = 0;
		//byte placedDisc = 0;
		int[] kx = new int[6];
		int[] ky = new int[6];
		int[] prevReg = null;
		int kd = -1;
		int kfail = 0;
		int[] popProbs = {1,2,3,5,8,13,8,13,8,5,3,2,1};
		// place the 6 "kingdoms" into 6 possibly overlapping population centers,
		// 6+1+6; 1/2/3/5/8/13/8/13/8/5/3/2/1  sum = 72
		//focusing on temperate zones  [6 capitals + 4-6 ports for each of them] [6+24-36][42 max]
		System.out.println("Adding POIs");
		for(int k = 0; k < 6; k++)
		{
			int nPorts = (int) (UWNHRando.rand() * 3 + 5);  //one for capitol + 4-6 normal ports
			int portsPlaced = 0;
			
			while(portsPlaced < nPorts)
			{
				//select a region
				int[] reg = selectRegion(prevReg, popProbs, population, true);
				int regx = reg[0];
				int regy = reg[1];
			
				int herePorts = (int) (UWNHRando.rand() * 3 + 1);
				if(herePorts > (nPorts - portsPlaced))
					herePorts = nPorts - portsPlaced;
				ArrayList<Point> newPois = placePorts(herePorts, regx, regy, allPois);
				//newPois contains the new port points 
				if(newPois == null)
				{
					//k--;
					kfail++;
					continue;
				}
				allPois.addAll(newPois);  //only interaction with allPois
				kfail = 0;
				for(int i = 0; i < herePorts; i++)  
				{
					//allPois.add(0, newPois.get(i));
					boolean cap = false;
					int[] sup = {0,0,0,0,0,0};
					int possCult = 3;  //0 and 1
					if(k == 2)
					{
						possCult += 15; //turn off 0, turn on 4
					}
					if(i == 0 && portsPlaced == 0)
					{
						cap = true;
						sup[k] = 100;
					}
					else
						sup[k] = (int) ((UWNHRando.rand() * 40) + 30);
					PortData pd = new PortData(possCult, cap, sup);
					pd.setXY(newPois.get(i).x, newPois.get(i).y);
					pd.setOpenings(this);
					byte ia = 6;
					if(sup[k] > 80)
						ia = (byte) k;
					ia += 16;
					if(cap)
						ia += 64;
					pd.setIAData(ia);
					pdata.add(pd);
					
					if(knownWorld == null)  //initialize the known world
					{
						 // this should happen ONCE
						knownWorld = new Rectangle();
						Point pp = newPois.get(i);
						knownWorld.x = pp.x - 12;
						knownWorld.y = pp.y - 12;
						knownWorld.width = 24;
						knownWorld.height = 24;
						System.out.println("Initializing known world to " + knownWorld.toString());
					}
					System.out.println("Placing KW port at " + newPois.get(i));
					if(!knownWorld.contains(newPois.get(i)))
					{
						Point pp = newPois.get(i);
						knownWorld.x = Math.min(knownWorld.x, pp.x - 12);
						knownWorld.y = Math.min(knownWorld.y, pp.y - 12);
						int x2 = pp.x + 12;
						int y2 = pp.y + 12;
						knownWorld.width = Math.max(knownWorld.width, x2 - knownWorld.x);
						knownWorld.height = Math.max(knownWorld.height, y2 - knownWorld.y);
						System.out.println("Adjusting known world to " + knownWorld.toString());
						
					}
				}
				/*for(int i = herePorts; i < newPois.size(); i++)
				{
					//add all other points at the end (again, exclusion radius)
					allPois.add(newPois.get(i));
				}*/
				portsPlaced += herePorts;
				placedPorts += herePorts;
				population[regx][regy] += 2 * herePorts;
				/*kx[k] = regx;
				ky[k] = regy;*/
				prevReg = reg;
				//kd = -1;
			}
		}
		System.out.println("Done adding Kingdom POIs");
		prevReg = null;
		//mark 3 additional cultural centers (these become India, Middle East and Far East), 
		//each with one cultural offshoot next to them  [4-7 ports/cultural region, 3-6 ports/offshoot] [18+15]
		//again, focus on temperate or tropical zones  [63/100 ports used total][81 max]
		for(int k = 0; k < 6; k++)
		{
			int nPorts = 0;
			if((k & 1) == 0)
				nPorts = (int) (UWNHRando.rand() * 5 + 3);  //3-7 normal ports in cultural center
			else
				nPorts = (int) (UWNHRando.rand() * 4 + 2);  //2-5 normal ports for cultural offshoot
			int portsPlaced = 0;
			while(portsPlaced < nPorts)
			{
				//select a region
				int[] reg = selectRegion(prevReg, popProbs, population, false);
				int regx = reg[0];
				int regy = reg[1];
			
				int herePorts = (int) (UWNHRando.rand() * 3 + 1);
				if(herePorts > (nPorts - portsPlaced))
					herePorts = nPorts - portsPlaced;
				ArrayList<Point> newPois = placePorts(herePorts, regx, regy, allPois);
				//newPois contains the new port points as well as all points that
				//must be excluded due to a port's exclusion radius
				if(newPois == null)
				{
					//k--;
					kfail++;
					if(portsPlaced == 0)
						prevReg = null;
					continue;
				}
				portsPlaced += herePorts;
				kfail = 0;
				allPois.addAll(newPois);
				for(int i = 0; i < herePorts; i++)
				{
					//allPois.add(0, newPois.get(i));
					boolean cap = false;
					int[] sup = {0,0,0,0,0,0};
					int possCult = 0;
					switch(k)
					{
					case 0: //India, supporter is Portugal(0)
						possCult = 64;
						sup[0] = (int) ((UWNHRando.rand() * 40) + 10);
						break;
					case 1:  //Spice Islands, supporter is Dutch(4)
						possCult = 96;
						sup[4] = (int) ((UWNHRando.rand() * 30));
						break;
					case 2:  //Middle East, supporter is Turkey(2)
						possCult = 16;
						sup[2] = (int) ((UWNHRando.rand() * 40) + 30);
						break;
					case 3:
						possCult = 24;
						break;
					case 4:  //Far East, supporter is Italy(5) (Marco Polo)
						possCult = 128;
						sup[5] = (int) ((UWNHRando.rand() * 20) + 10);
						break;
					case 5:   //Chinese, Japanese, or Spice Islands
						possCult = 416;
						break;
					}
					PortData pd = new PortData(possCult, cap, sup);
					pd.setXY(newPois.get(i).x, newPois.get(i).y);
					pd.setOpenings(this);
					//pd.setIAData((byte) 6);
					pdata.add(pd);
				}
				//for(int i = herePorts; i < newPois.size(); i++)
					//allPois.add(newPois.get(i));
				placedPorts += herePorts;
				population[regx][regy] += 2 * herePorts;
				kx[k] = regx;
				ky[k] = regy;
			}
		}
		System.out.println("Done adding cultural center POIs");
		//other ports can be anywhere
		while(placedPorts < 100)
		{
			int regx = 0;
			int regy = 0;
			
			int r = (int) (UWNHRando.rand() * 72);
			regy = 0;
			for(int i = 0; i < popProbs.length; i++)
			{
				r -= popProbs[i];
				if(r <= 0)
					break;
				regy++;
			}
			regx = (int) (UWNHRando.rand() * 27);
			
			if(population[regx][regy] >= 6)
				continue;
			
			int nPorts = (int) (UWNHRando.rand() * 2 + 1);
			nPorts = Math.min(nPorts, 100 - placedPorts);
			ArrayList<Point> newPois = placePorts(nPorts, regx, regy, allPois);
			if(newPois == null)
				continue;
			allPois.addAll(newPois);
			for(int i = 0; i < nPorts; i++)
			{
				//allPois.add(0, newPois.get(i));
				boolean cap = false;
				int[] sup = {0,0,0,0,0,0};
				int possCult = 44;  //2,3,5
				PortData pd = new PortData(possCult, cap, sup);
				pd.setXY(newPois.get(i).x, newPois.get(i).y);
				pd.setOpenings(this);
				pdata.add(pd);
			}
			//for(int i = nPorts; i < newPois.size(); i++)
				//allPois.add(newPois.get(i));
			placedPorts += nPorts;
			population[regx][regy] += 2 * nPorts;
		}
		System.out.println("Done adding remaining port POIs");
		
		
		
		ArrayList<Point> zeroPop = new ArrayList<Point>();
		for(int rx = 0; rx < 27; rx++)
			for(int ry = 0; ry < 13; ry++)
				if(population[rx][ry] == 0)
					zeroPop.add(new Point(rx, ry));
		
		//place 30 supply ports in zero population regions
		for(int i = 0; i < 30; i++)
		{
			int r = (int) (UWNHRando.rand() * zeroPop.size());
			Point rp = zeroPop.get(r);
			int regx = rp.x;
			int regy = rp.y;
			int nPorts = 1;
			ArrayList<Point> newPois = placePorts(nPorts, regx, regy, allPois);
			if(newPois == null)
			{
				i--;
				zeroPop.remove(r);
				continue;
			}
			for(int j = 0; j < nPorts; j++)
				allPois.add(0, newPois.get(j));
			//for(int j = nPorts; j < newPois.size(); j++)
				//allPois.add(newPois.get(j));
			PortData pd = new PortData(-1, false, null);
			pd.setSupplyOnly(true);
			pd.setXY(newPois.get(0).x, newPois.get(0).y);
			pd.setOpenings(this);
			pdata.add(pd);
			population[regx][regy]++;
			zeroPop.remove(r);
		}
		System.out.println("done adding supply port POIs");
		//Anything else is a backwater or undiscovered point, ripe for discoveries
		//pepper the map with the 99 discoveries, focusing on undiscovered points and reaching into
		//population centers
		for(int i = 0; i < 99; i++)
		{
			int regx = (int) (UWNHRando.rand() * 27);
			int regy = (int) (UWNHRando.rand() * 13);
			int r = (int) (UWNHRando.rand() * 10);  //roll a d10; be greater than the population of region
			if(r <= population[regx][regy])  //re-roll location
			{
				i--;
				continue;
			}
			ArrayList<Point> newPois = placePorts(1, regx, regy, allPois);
			if(newPois == null)
			{
				i--;
				continue;
			}
			for(int j = 0; j < 1; j++)
				allPois.add(0, newPois.get(j));
			//for(int j = 1; j < newPois.size(); j++)
				//allPois.add(newPois.get(j));
			PortData pd = new PortData(-1, false, null);
			pd.setDiscovery(true);
			pd.setXY(newPois.get(0).x, newPois.get(0).y);
			pd.setOpenings(this);
			pdata.add(pd);
		}
		System.out.println("Done adding discovery POIs");
		
		byte discovery = (byte) 196;
		byte supplyPort = (byte) 192;
		byte fullPort = (byte) 188;
		for(int i = 0; i < 229; i++)  //99 discoveries, 30 supply ports, 100 full ports
		{
			Point p = allPois.get(i);
			if(i < 99)
				fullMap[p.y][p.x] = discovery;
			else if(i < 129)
				fullMap[p.y][p.x] = supplyPort;
			else
				fullMap[p.y][p.x] = fullPort; 
			fullMap[p.y][p.x + 1] = (byte) (fullMap[p.y][p.x] + 1); 
			fullMap[p.y + 1][p.x] = (byte) (fullMap[p.y][p.x] + 2);
			fullMap[p.y + 1][p.x + 1] = (byte) (fullMap[p.y][p.x] + 3);
		}
		allPortData = pdata;
		/*for(int i = 0; i < 100; i++)
			if(allPortData.get(i).isSupply)
				System.out.println("Supply port found at index " + i);*/
		System.out.println("Done inserting POIs");
		
	}
	
	public void makeMap(int nContinents, int nIslands, int polarThickness)
	{
		int cWidth = (int) (1800 / nContinents);
		int cHeight = 900;/* / Math.max(1, (int) Math.floor(nContinents / 2)); */
		int halfC = nContinents;
		int vslack = 100;
		byte solids = 0;
		if(nContinents > 3)
		{
			cHeight = 450;
			halfC = nContinents / 2;
			vslack  = 50;
			cWidth *= 2;
		}
		int hslack = 300 / halfC;
		ArrayList<Rectangle> areas = new ArrayList<Rectangle>();
		for(int i = 0; i < nContinents; i++)
		{
			int r1 = (int) (UWNHRando.rand() * 25);
			int rcw = (int) (((r1 + 75.0) / 100.0) * cWidth);
			int r2 = (int) (UWNHRando.rand() * 25);
			int rch = (int) (((r2 + 75.0) / 100.0) * cHeight);
			int r3 = (int) (UWNHRando.rand() * 25);
			int xx = (int) ((cWidth * (i % halfC)) + (UWNHRando.rand() * hslack));
			int rx = (int) (((r3 + 87.0) / 100.0) * xx);
			int r4 = (int) (UWNHRando.rand() * 25);
			int yy = (int) ((cHeight * (i / halfC)) + (UWNHRando.rand() * vslack));
			int ry = (int) (((r4 + 87.0) / 100.0) * yy);
			Rectangle cr = new Rectangle(rx, ry, rcw, rch);
			System.out.println("Making continent #" + i + " rect=" + rx + "," + ry + "," + rcw + "," + rch);
			if(solids == 2)
			{
				solids += makeContinent(cr, true);
				solids = 0;
			}
			else
			{
				solids += makeContinent(cr, false);
			}
			areas.add(cr);
		}
		for(int i = 0; i < nIslands; i++)
		{
			int fail = 0;
			while(fail < 1000)
			{
				int tx = (int) (UWNHRando.rand() * 2150);
				int ty = (int) (UWNHRando.rand() * 1050);
				Rectangle tr = new Rectangle(tx, ty, 20, 20);
				if(testAgainstList(tr, areas) == false)
				{
					fail++;
					continue;
				}
				//System.out.println("Making island #" + i + " rect=" + tr.x + "," + tr.y + "," + tr.width + "," + tr.height);
				tr = growRect(tr, areas, fullMap[0].length - 1, fullMap.length - 1);
				System.out.println("Making island #" + i + " rect=" + tr.x + "," + tr.y + "," + tr.width + "," + tr.height);
				if(tr.width >= 80 && tr.height >= 80 && UWNHRando.rand() > 0.5)
					makeArchipelago(tr);
				else
					makeIsland(tr);
				areas.add(tr);
				break;
			}
		}
		smoothWater(8);
		backup();
		//System.out.println("Done raising water");
		//GameMapWindow gmw = new GameMapWindow(this);
		makePrelimIceCaps(polarThickness);
		raiseLand();
		//one more smoothWater
		smoothWater(8);
		makeIceCaps(polarThickness);
		applyTerrain();
		simplifyInvisible();
		mountainAndForestFix();
		raiseLand();
		
		boolean[][] oceans = selectOceans(polarThickness);
		okForPOI = getPOIPoints(oceans);
		//GameMapWindow gmw = new GameMapWindow(this);
		
		addPOIs();
		backup();
	}
	
	private void makePrelimIceCaps(int polarThickness) 
	{
		int w1 = polarThickness * 2;
		for(int y = 0; y < w1; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				fullMap[y][x] = 1;
				/*	fullMap[y][x] = 1;
				else
					fullMap[y][x] = 0;*/
				fullMap[fullMap.length - 1 - y][x] = 1;
			}
		}
		
	}

	public void makeIceCaps(int width) 
	{
		int w1 = width;
		int w2 = width;
		
		for(int y = 0; y < 2 * width; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				fullMap[y][x] = fullMap[2 * width][x];
				/*	fullMap[y][x] = 1;
				else
					fullMap[y][x] = 0;*/
				fullMap[fullMap.length - 1 - y][x] = fullMap[fullMap.length - 1 - 2 * width][x];
			}
		}
		
		for(int i = 0; i < fullMap[0].length; i++)
		{
			int diff = 0;
			double r1 = UWNHRando.rand();
			if(r1 > 0.3)
				diff++;
			if(r1 > 0.6)
				diff++;
			if(r1 > 0.9)
				diff++;
			if(UWNHRando.rand() < 0.5)
			{
				w1 += diff;
				if(w1 > width * 2)
					w1 = width * 2;
			}
			else
			{
				w1 -= diff;
				if(w1 < 1)
					w1 = 1;
			}
			diff = 0;
			r1 = UWNHRando.rand();
			if(r1 > 0.3)
				diff++;
			if(r1 > 0.6)
				diff++;
			if(r1 > 0.9)
				diff++;
			if(UWNHRando.rand() < 0.5)
			{
				w2 += diff;
				if(w2 > width * 2)
					w2 = width * 2;
			}
			else
			{
				w2 -= diff;
				if(w2 < 1)
					w2 = 1;
			}
		
			for(int j = 0; j < w1; j++)
				fullMap[j][i] = 1;
			for(int j = 0; j < w2; j++)
				fullMap[fullMap.length - 1 - j][i] = 1;
		}
	}

	private void applyTerrain() 
	{
		int step = 32;
		byte[][] selected = new byte[fullMap.length][];
		for(int i = 0; i < selected.length; i++)
			selected[i] = new byte[fullMap[0].length];
		int[] maxCats = {0,0,0};
		for(int y = 0; y < fullMap.length; y += step)
		{
			for(int x = 0; x < fullMap[0].length; x += step)
			{
				if(fullMap[y][x] != 1)
					continue;
				if(selected[y][x] != 0)
					continue;
				//get max dist to water
				int[] dists = distToWater(x, y, 8);
				int max = 0;
				for(int i = 0; i < dists.length; i++)
					if(dists[i] > max)
						max = dists[i];
				double[] probs = {0.1, 0.5, 0.15, 0.25};
				maxCats[0]++;
				if(max < 8)
				{
					double[] p2 = {0.2, 0.5, 0.3, 0.0};
					probs = p2;
					maxCats[0]--;
					maxCats[1]++;
				}
				if(max < 5)
				{
					double[] p2 = {0.5, 0.5, 0.0, 0.0};
					probs = p2;
					maxCats[1]--;
					maxCats[2]++;
				}
				Point pt = new Point(x, y);
				byte terr = 0;
				double r = UWNHRando.rand();
				for(int i = 0; i < probs.length; i++)
				{
					r -= probs[i];
					if(r < 0)
					{
						terr = (byte) (i + 1);
						break;
					}
				}
				ArrayList<Point> pts = floodPoint(pt, selected, (byte) 1, step >> 4, step >> 4, 0.001, 0.49);
				for(int i = 0; i < pts.size(); i++)
				{
					Point p = pts.get(i);
					if(fullMap[p.y][p.x] != 0)
						fullMap[p.y][p.x] = terr;
				}
				
			}
		}
		//finally fix mountains and trees  - trees are 2x1, mountains are 2x2
		//mountainAndForestFix();
		//System.out.println("Max categories=" + Arrays.toString(maxCats));
		
	}
	
	public void mountainAndForestFix()
	{
		int mountainCount = 0;
		int forestCount = 0;
		int desertCount = 0;
		for(int y = 0; y < fullMap.length; y+=2)  //mountain loop
		{
			for(int x = 0; x < fullMap[0].length; x+=2)
			{
				if(y < 24 || y >= 1056)
				{
					if(fullMap[y][x] != 0)
						fullMap[y][x] = 1;
					if(fullMap[y + 1][x] != 0)
						fullMap[y + 1][x] = 1;
					if(fullMap[y][x + 1] != 0)
						fullMap[y][x + 1] = 1;
					if(fullMap[y + 1][x + 1] != 0)
						fullMap[y + 1][x + 1] = 1;
					continue;
				}
				if(fullMap[y][x] == 4 || fullMap[y+1][x] == 4 || fullMap[y][x+1] == 4 || fullMap[y+1][x+1] == 4)  //mountain
				{
					fullMap[y][x] = 4;
					fullMap[y][x+1] = 4;
					fullMap[y+1][x] = 4;
					fullMap[y+1][x+1] = 4;
					mountainCount++;
				}
				if(fullMap[y][x] == 3 || fullMap[y+1][x] == 3 || fullMap[y][x+1] == 3 || fullMap[y+1][x+1] == 3)  //deserts
				{
					fullMap[y][x] = 3;
					fullMap[y][x+1] = 3;
					fullMap[y+1][x] = 3;
					fullMap[y+1][x+1] = 3;
					desertCount++;
				}
			}
		}
		for(int y = 24; y < fullMap.length; y+=2)  //tree loop
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(y < 24 || y >= 1056)
				{
					if(fullMap[y][x] != 0)
						fullMap[y][x] = 1;
					continue;
				}
				if(fullMap[y][x] == 2 || fullMap[y+1][x] == 2)  //tree
				{
					fullMap[y][x] = 2;
					fullMap[y+1][x] = 2;
					forestCount++;
				}
			}
		}
		System.out.println("--------------------------");
		System.out.println("Mountain count=" + mountainCount);
		System.out.println("Forest count=" + forestCount);
		System.out.println("Desert count=" + desertCount);
		System.out.println("--------------------------");
	}

	private void raiseLand()
	{
		boolean[][] selected = new boolean[fullMap.length][];
		for(int i = 0; i < selected.length; i++)
			selected[i] = new boolean[fullMap[0].length];
		int[] xs = {0, fullMap[0].length / 2, 0, fullMap[0].length / 2};
		int[] ys = {0, 0, fullMap.length - 1, fullMap.length - 1};
		for(int i = 0; i < 2; i++)
		{
			while(fullMap[ys[i]][xs[i]] == 1)
				ys[i]++;
		}
		for(int i = 2; i < 4; i++)
		{
			while(fullMap[ys[i]][xs[i]] == 1)
				ys[i]--;
		}
		
		Point[] pts = new Point[4];
		for(int i = 0; i < 4; i++)
			pts[i] = new Point(xs[i], ys[i]);
		ArrayList<Point> flood = new ArrayList<Point>();
		for(int i = 0; i < pts.length; i++)
		{
			Point p = pts[i];
			if(fullMap[p.y][p.x] == 1)
				continue;
			//IndexedPoint ip = new IndexedPoint(p);
			if(flood.contains(p))  
				continue;
			flood.addAll(floodSelect(p, (byte) 1, false, selected));  //n log n
		}
		
		/*for(int i = 0; i < flood.size(); i++)
		{
			Point p = flood.get(i);
			selected[p.y][p.x] = true;
		}*/
		//for each point not selected, it becomes land
		for(int y = 0; y < selected.length; y++)
		{
			for(int x = 0; x < selected[y].length; x++)
			{
				if(selected[y][x] == false && fullMap[y][x] != 1)
					fullMap[y][x] = 1;
			}
		}
	}
	
	private IndexedList[][] getPOIPoints(boolean[][] oceans)
	{
		IndexedList[][] rv = new IndexedList[27][];
		int my = oceans.length - 1;
		int mx = oceans[0].length - 1;
		for(int i = 0; i < rv.length; i++)
		{
			rv[i] = new IndexedList[13];
			for(int j = 0; j < 13; j++)
				rv[i][j] = new IndexedList();
		}
		int lx = 0;
		int ly = 0;
		int px = 0;
		int py = 0;
		for(int yy = 0; yy < oceans.length; yy++)
		{
			for(int xx = 0; xx < oceans[yy].length; xx++)
			{
				if(oceans[yy][xx])
				{
					Point p = new Point(xx, yy);
					ArrayList<Point> pts = getSurr4(p, mx, my, true);
					for(int i = 0; i < pts.size(); i++)
					{
						Point pt = pts.get(i);
						px = pt.x;
						py = pt.y;
						if(fullMap[py][px] != 0 && py >= 20 && py < 1060)
						{
							lx = px / 80;
							ly = (py - 20) / 80;
							rv[lx][ly].add(new IndexedPoint(pt));
							
						}
					}
				}
			}
		}
		return rv;
	}
	
	private boolean[][] selectOceans(int iceCap)
	{
		//count up water tiles
		int water = 0;
		for(int yy = 0; yy < fullMap.length; yy++)
			for(int xx = 0; xx < fullMap[yy].length; xx++)
				if(fullMap[yy][xx] == 0)
					water++;
		
		int[] xs = {3, 1081, 3, 1081};
		int[] ys = {iceCap, 1080 - iceCap, 1080 - iceCap, iceCap};  //4 corners testing
		int largest = 0;
		int[] bounds = {0,0,0,0};
		boolean[][] out = null;
		for(int i = 0; i < xs.length; i++)
		{
			boolean[][] rv = new boolean[1080][2160];
			ArrayList<Point> ocean = new ArrayList<Point>();
			//test 2 spots
			int x = xs[i];
			int y = ys[i];
			do
			{
				if(i == 0 || i == 3)
					y += 2;
				else
					y -= 2;
				Point st = new Point(x, y);
				ocean = floodSelect2(st, (byte) 0, true, rv);
			}while(ocean == null);
			if(ocean.size() - 2 > water / 2)  //ocean size > half the world's water
			{
				return rv;
			}
			else if(ocean.size() == largest)  //== largest and same bounds then accept it
			{
				Point min = ocean.get(largest - 2);
				Point max = ocean.get(largest - 1);
				int[] bb = {min.x, min.y, max.x, max.y};
				if(Arrays.equals(bb, bounds))
					return rv;
			}
			if(ocean.size() > largest)
			{
				largest = ocean.size();
				out = rv;
				Point min = ocean.get(largest - 2);
				Point max = ocean.get(largest - 1);
				int[] bb = {min.x, min.y, max.x, max.y};
				bounds = bb;
			}
			
		}
		return out;
		
	}
	
	//2 point flood select expansion; both values must match going in a given direction to expand in that direction
	private ArrayList<Point> floodSelect2(Point st, byte val, boolean matchVal, boolean[][] selected)
	{
		if(matchVal)
		{
			if(fullMap[st.y][st.x] != val)
				return null;
			if(fullMap[st.y + 1][st.x] != val)
				return null;
			if(fullMap[st.y][st.x + 1] != val)
				return null;
			if(fullMap[st.y + 1][st.x + 1] != val)
				return null;
		}
		ArrayList<Point> rv = new ArrayList<Point>();
		rv.add(st);
		rv.add(new Point(st.x, st.y + 1));
		rv.add(new Point(st.x + 1, st.y));
		rv.add(new Point(st.x + 1, st.y + 1));
		int expanded = 0;
		int xm = fullMap[0].length - 1;
		int ym = fullMap.length - 1;
		int minX = 9999;
		int maxX = 0;
		int minY = 9999;
		int maxY = 0;
		while(expanded < rv.size())
		{
			Point p = rv.get(expanded);
			ArrayList<Point> ls = getSurr8(p, xm, ym, true);
			for(int i = 0; i < ls.size(); i++)
			{
				Point pp = ls.get(i);
				/*byte good = 0;
				byte wall = 0;*/
				if(matchVal)
				{
					if(fullMap[pp.y][pp.x] != val || selected[pp.y][pp.x])
					{
						ls.remove(i);
						if((i & 1) == 0)
							ls.remove(i);  //remove next
						else
						{
							ls.remove(i - 1);  //remove prev
							i--;
						}
						i--;
						//good = 0;
						//System.out.print(ls.size());
					}
					/*else
						good++;
					if(good == 2)
					{
						for(int k = 0; k < wall; k++)
						selected[pp.y][pp.x] = true;
						if(pp.x < minX)
							minX = pp.x;
						if(pp.x > maxX)
							maxX = pp.x;
						if(pp.y < minY)
							minY = pp.y;
						if(pp.y > minY)
							minY = pp.y;
					}*/
				}
				else
				{
					if(fullMap[pp.y][pp.x] == val || selected[pp.y][pp.x])
					{
						ls.remove(i);
						if((i & 1) == 0)
							ls.remove(i);
						else
						{
							ls.remove(i - 1);
							i--;
						}
						i--;
					}
					/*else
					{
						
					}*/
				}
			}
			for(int i = 0; i < ls.size(); i++)
			{
				Point pp = ls.get(i);
				selected[pp.y][pp.x] = true;
				if(pp.x < minX)
					minX = pp.x;
				if(pp.x > maxX)
					maxX = pp.x;
				if(pp.y < minY)
					minY = pp.y;
				if(pp.y > minY)
					minY = pp.y;
			}
			//System.out.println();
			rv.addAll(ls);
			expanded++;
		}
		Point p1 = new Point(minX, minY);
		rv.add(p1);
		Point p2 = new Point(maxX, maxY);
		rv.add(p2);
		return rv;
	}
	
	private ArrayList<Point> floodSelect(Point st, byte val, boolean matchVal, boolean[][] selected)
	{
		//IndexedList rv = new IndexedList();
		ArrayList<Point> qq = new ArrayList<Point>();
		qq.add(st);
		selected[st.y][st.x] = true;
		int expanded = 0;
		int xm = fullMap[0].length - 1;
		int ym = fullMap.length - 1;
		while(expanded < qq.size())   //n
		{
			Point p = qq.get(expanded);
			ArrayList<Point> ls = getSurr4(p, xm, ym, true);
			for(int i = 0; i < ls.size(); i++)
			{
				Point pp = ls.get(i);
				if(matchVal)
				{
					if(fullMap[pp.y][pp.x] != val || selected[pp.y][pp.x])
					{
						ls.remove(i);
						i--;
					}
					else
						selected[pp.y][pp.x] = true;
				}
				else
				{
					if(fullMap[pp.y][pp.x] == val || selected[pp.y][pp.x])
					{
						ls.remove(i);
						i--;
					}
					else
						selected[pp.y][pp.x] = true;
				}
			}
			qq.addAll(ls);
			expanded++;
		}
		/*for(int i = 0; i < qq.size(); i++)  //n*lgn
		{
			Point pp = qq.get(i);
			IndexedPoint ip = new IndexedPoint(pp);
			rv.add(ip);
		}*/
		return qq;
	}
	
	private int[] distToLand(int x, int y, byte tp)   //gets distance to nearest land that is not equal to tp
	{
		int[] rv = {0,0,0,0};
		int i = 1;
		if(fullMap[y][x] != tp)
			return rv;
		//int sum = 0;
		//int i = 0;
		while(i <= 8)  //up
		{
			if((y - i) < 0)
				break;
			if(fullMap[y - i][x] != tp)
				break;
			i++;
		}
		
		rv[0] = i;
		//sum += i;
		i = 1;
		while(i <= 8)  //dn
		{
			if((y + i) >= fullMap.length)
				break;
			if(fullMap[y + i][x] != tp)
				break;
			i++;
		}
		//sum += i;
		rv[1] = i;
		i = 1;
		int xx = 0;
		while(i <= 8)  //lf
		{
			xx = x - i;
			if(xx < 0)
				xx += fullMap[0].length;
			if(fullMap[y][xx] != tp)
				break;
			i++;
		}
		rv[2] = i;
		//sum += i;
		i = 1;
		while(i <= 8)  //rt
		{
			xx = x + i;
			if(xx >= fullMap[0].length)
				xx -= fullMap[0].length;
			if(fullMap[y][xx] != tp)
				break;
			i++;
		}
		rv[3] = i;
		return rv;
	}

	public int[] distToWater(int x, int y, int max) //gets the distance to nearest water
	{
		int[] rv = {0,0,0,0};
		/*for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{*/
				int i = 1;
				if(fullMap[y][x] == 0)
					return rv;
				//int sum = 0;
				//int i = 0;
				while(i < max)  //up
				{
					if((y - i) < 0)
						break;
					if(fullMap[y - i][x] == 0)
						break;
					i++;
				}
				
				rv[0] = i;
				//sum += i;
				i = 1;
				while(i < max)  //dn
				{
					if((y + i) >= fullMap.length)
						break;
					if(fullMap[y + i][x] == 0)
						break;
					i++;
				}
				//sum += i;
				rv[1] = i;
				i = 1;
				int xx = 0;
				while(i < max)  //lf
				{
					xx = x - i;
					if(xx < 0)
						xx += fullMap[0].length;
					if(fullMap[y][xx] == 0)
						break;
					i++;
				}
				rv[2] = i;
				//sum += i;
				i = 1;
				while(i < max)  //rt
				{
					xx = x + i;
					if(xx >= fullMap[0].length)
						xx -= fullMap[0].length;
					if(fullMap[y][xx] == 0)
						break;
					i++;
				}
				rv[3] = i;
				return rv;
				/*sum += i;
				if(sum < 12)  //average must be >= 3
					fullMap[y][x] = 0;*/
				
			//}
		//}
	}
	
	class CompressionChunk
	{
		byte[] chunk;
		int index;
		int loc;
		int zone;
		
		CompressionChunk(byte[] bits, int index, int loc, int zone)
		{
			chunk = bits;
			this.index = index;
			this.loc = loc;
			this.zone = zone;   //0= main map loc, 1= data overflow, 2=minimap tile region, 3=town tile region
		}
		
		public void move(int newLoc, int newZone)
		{
			loc = newLoc;
			zone = newZone;
		}
	}
	
	CompressionChunk[] chunks;
	
	public void saveToRom(SNESRom rom, int[][] deltaIndeces)  //use this function to save the map
	{
		int deltaLoc = 249786 - 512;  //3cfba
		for(int yy = 0; yy < deltaIndeces.length; yy++)
		{
			for(int xx = 0; xx < deltaIndeces[0].length; xx++)
			{
				int idx = deltaIndeces[yy][xx];
				int delta = chunks[idx].loc;
				rom.write3Bytes(deltaLoc, delta);
				deltaLoc += 3;
			}
		}
		for(int i = 0; i < chunks.length; i++)
		{
			//then dump the bytes
			int writeLoc = chunks[i].loc + rom.MAP_LOC;
			for(int j = 0; j < chunks[i].chunk.length; j++)
			{
				rom.data[writeLoc] = chunks[i].chunk[j];
				writeLoc++;
			}
		}
	}
	
	public int smoothWater(int maxLen)
	{
		int elim = 0;
		int min = (maxLen * 2) + 2;
		min = Math.min(16, min);
		for(int y = 0; y < fullMap.length; y++)
		{
			for(int x = 0; x < fullMap[0].length; x++)
			{
				if(fullMap[y][x] == 0)
					continue;
				int[] ds = distToWater(x, y, maxLen);
				int sum = 0;
				for(int i = 0; i < 4; i++)
					sum += ds[i];
				if(sum < min)
				{
					fullMap[y][x] = 0;
					elim++;
				}
				else if(sum == min)
				{
					if((ds[0] + ds[1] < 3) || (ds[2] + ds[3] < 3))
					{
						fullMap[y][x] = 0;
						elim++;
					}
				}
			}
		}
		return elim;
	}
	
	class BlockSizeIndexer implements Indexed
	{
		int size;
		int blkIndex;
		
		BlockSizeIndexer(int bi, int sz)
		{
			blkIndex = bi;
			size = sz;
		}

		public long getIndex() 
		{
			return size;
		}
	}
	
	class IndexedPoint implements Indexed
	{
		Point pt;
		
		IndexedPoint(Point p)
		{
			pt = p;
		}
		
		public long getIndex()
		{
			return pt.y * 5000 + pt.x;
		}
	}

	public void simplifyBlocks(int nBlocks, ArrayList<ArrayList<Byte>> bts, SNESRom rom) 
	{
		IndexedList il = new IndexedList();
		il.allowDuplicates(true);
		for(int i = 0; i < bts.size(); i++)
		{
			ArrayList<Byte> blk = bts.get(i);
			BlockSizeIndexer bsi = new BlockSizeIndexer(i, blk.size());
			il.add(bsi);
		}
		Compressor comp = new Compressor(rom);
		for(int i = 0; i < nBlocks; i++)
		{
			int idx = il.size() - 1 - i;
			int bindex = ((BlockSizeIndexer) il.get(idx)).blkIndex;
			ArrayList<Byte> blk = bts.get(bindex);
			byte[] blka = new byte[blk.size()];
			for(int j = 0; j < blk.size(); j++)
				blka[j] = blk.get(j);
			ArrayList<Byte> dcomp = comp.decompress(0, blka);
			byte[][] tile = new byte[12][12];
			for(int y = 0; y < 12; y++)
				for(int x = 0; x < 12; x++)
					tile[y][x] = dcomp.remove(0);
			tile = simplifyBlock(tile);
		} 
	}
	
	private byte[][] simplifyBlock(byte[][] tile)
	{
		byte[] corns = {tile[0][0], tile[0][11], tile[11][0], tile[11][11]};
		byte water = 0;
		byte waterCorns = 4;
		for(int i = 0; i < corns.length; i++)
		{
			if(corns[i] != 0)
			{
				water |= (1 << i);
				waterCorns--;
			}
		}
		if(waterCorns == 4)
		{
			byte[] wline = {0,0,0,0};
			byte[] wline2 = {0,0,0,0};
			for(int i = 0; i < 12; i++)
			{
				if(tile[0][i] == 0)
					wline[0]++;
				if(tile[i][0] == 0)
					wline[1]++;
				if(tile[11][i] == 0)
					wline[2]++;
				if(tile[i][11] == 0)
					wline[3]++;
			}
			if(wline[0] == 12 && wline[1] == 12 && wline[2] == 12 && wline[3] == 12)  //island - sink it
			{
				for(int y = 0; y < 12; y++)
					for(int x = 0; x < 12; x++)
						tile[y][x] = 0;
				return tile;
			}
			for(int i = 11; i >= 0; i--)
			{
				if(tile[0][i] == 0)
					wline2[0]++;
				if(tile[i][0] == 0)
					wline2[1]++;
				if(tile[11][i] == 0)
					wline2[2]++;
				if(tile[i][11] == 0)
					wline2[3]++;
			}
			
		}
		return tile;
	}

	public void dumpMap(SNESRom rom, int[][] deltaIndeces)
	{
		rom.mapLocs[1] = SNESRom.DATA_OVERFLOW + rom.dataUsed;
		int[] outLocs = Arrays.copyOf(rom.mapLocs, rom.mapLocs.length);
		int[] zoneCounts = new int[4];
		int deltaLoc = 249786 - 512;
		for(int i = 0; i < chunks.length; i++)
		{
			CompressionChunk cc = chunks[i];
			int z = chunks[i].zone;
			rom.writeBytes(outLocs[z], cc.chunk);
			/*int loc = outLocs[z] - SNESRom.MAP_LOC;
			rom.write3Bytes(deltaLoc, loc);*/
			cc.loc = outLocs[z];
			outLocs[z] += cc.chunk.length;
			zoneCounts[z]++;
			//deltaLoc += 3;
		}
		for(int yy = 0; yy < deltaIndeces.length; yy++)
		{
			for(int xx = 0; xx < deltaIndeces[yy].length; xx++)
			{
				int idx = deltaIndeces[yy][xx];
				int loc = chunks[idx].loc - SNESRom.MAP_LOC;
				rom.write3Bytes(deltaLoc, loc);
				deltaLoc += 3;
			}
		}
		System.out.println("Zone starts=" + Arrays.toString(rom.mapLocs));
		System.out.println("Zone ends=" + Arrays.toString(outLocs));
		System.out.println("Zone counts=" + Arrays.toString(zoneCounts));
	}

	public byte[] getCompressedMap() 
	{
		if(chunks == null)
			return null;
		else
		{
			int sz = 0;
			for(int i = 0; i < chunks.length; i++)
				sz += chunks[i].chunk.length;
			byte[] rv = new byte[sz];
			int w = 0;
			for(int i = 0; i < chunks.length; i++)
			{
				byte[] c = chunks[i].chunk;
				for(int j = 0; j < c.length; j++)
				{
					rv[w] = c[j];
					w++;
				}
			}
			return rv;
		}
	}
}

interface Indexed
{
	public long getIndex();
}

class IndexedInteger implements Indexed, Comparable<IndexedInteger>
{
	int val;
	IndexedInteger(int value)
	{
		val = value;
	}
	
	public long getIndex()
	{
		return val;
	}
	
	public String toString(boolean isntSuper)
	{
		return String.valueOf(val);
	}

	@Override
	public int compareTo(IndexedInteger o) 
	{
		return (int) (getIndex() - o.getIndex());
	}

	
}

class IndexedList
{
	ArrayList<Indexed> list;
	boolean allowDuplicates;
	
	//String removeLog;
	
	public IndexedList()
	{
		list = new ArrayList<Indexed>();
		allowDuplicates = false;
	}
	
	public IndexedList(int initSize)
	{
		list = new ArrayList<Indexed>(initSize);
		allowDuplicates = false;
	}
	

	public IndexedList(IndexedList otherList)
	{
		populate(otherList);
		allowDuplicates = false;
	}
	
	public void allowDuplicates(boolean value)
	{
		allowDuplicates = value;
	}
	
	public void remove(int i) 
	{
		// TODO Auto-generated method stub
		list.remove(i);
	}
	
	public ArrayList<Indexed> getList()
	{
		return list;
	}

	private IndexedList(ArrayList<Indexed> l)  //do not make this public
	{
		list = l;
		allowDuplicates = false;
	}
	
	public IndexedList copy() 
	{
		ArrayList<Indexed> al = (ArrayList<Indexed>) list.clone();
		return new IndexedList(al);
	}

	public int size() 
	{
		// TODO Auto-generated method stub
		return list.size();
	}

	public void clear() 
	{
		// TODO Auto-generated method stub
		list.clear();
	}
	
	public int addAll(IndexedList o)
	{
		int a = list.size();
		ArrayList<Indexed> nl = new ArrayList<Indexed>(a + o.size());
		int i = 0;
		int j = 0;
		long ii = 0;
		long jj = 0;
		while(true)  //simple merge
		{
			if(i == list.size())
			{
				nl.addAll(j, o.list);
				break;
			}
			else if(j == list.size())
			{
				nl.addAll(i, list);
				break;
			}
			else
			{
				ii = list.get(i).getIndex();
				jj = o.list.get(j).getIndex();
				if(ii < jj)
				{
					nl.add(list.get(i));
					i++;
				}
				else if(ii == jj)
				{
					if(allowDuplicates)
					{
						nl.add(list.get(i));
						i++;
					}
				}
				else
				{
					nl.add(o.list.get(j));
					j++;
				}
			}
		}
		list = nl;
		return nl.size();
	}
	
	public int add(Indexed newObj)
	{
		int idx = binSearch(newObj.getIndex(), 0, list.size() - 1);
		return internalAdd(idx, newObj);
	}
	
	public int append(Indexed newObj)
	{
		if(list.size() == 0 || newObj.getIndex() > list.get(list.size() - 1).getIndex())
		{
			list.add(newObj);
			return list.size() - 1;
		}
		else
		{
			int idx = binSearch(newObj.getIndex(), 0, list.size() - 1);
			return internalAdd(idx, newObj);
		}
	}
	
	public int addEarly(Indexed newObj)
	{
		int idx = expSearch(newObj.getIndex(), 0);
		return internalAdd(idx, newObj);
	}
	
	private int internalAdd(int idx, Indexed newObj)
	{
		if(list.size() == idx)
		{
			list.add(newObj);
			return idx;
		}
		if(list.get(idx).getIndex() != newObj.getIndex() || allowDuplicates)
		{
			list.add(idx, newObj);
			return idx;
		}
		return -1;
	}
	
	private int duplicateFind(Indexed newObj, int idx)
	{
		int i = idx;
		//removeLog += "\n Looking for " + newObj.toString() + "  index " + newObj.getIndex() + " going backwards from " + i;
		while(i >= 0)
		{
			//removeLog += "\n" + i + "=" + list.get(i).toString() + "  index is " + list.get(i).getIndex();
			if(list.get(i) == newObj)
				return i;
			if(list.get(i).getIndex() != newObj.getIndex())
				break;
			i--;
		}
		i = idx + 1;
		//removeLog += "\n going forward from " + i;
		while(i < list.size())
		{
			//removeLog += "\n" + i + "=" + list.get(i).toString() + "  index is " + list.get(i).getIndex();
			if(list.get(i) == newObj)
				return i;
			if(list.get(i).getIndex() != newObj.getIndex())
				break;
			i++;
		}
		return -1;
	}
	
	private int returnIndex(Indexed newObj, int idx)
	{
		if(idx >= list.size())
			return -1;
		if(list.get(idx).getIndex() == newObj.getIndex())
		{
			if(allowDuplicates)
				return duplicateFind(newObj, idx);
			else
				return idx;
		}
		return -1;
	}
	
	public int indexOf(Indexed newObj)
	{
		int idx = binSearch(newObj.getIndex(), 0, list.size() - 1);
		return returnIndex(newObj, idx);
	}
	
	public int indexOfExp(Indexed newObj)
	{
		int idx = expSearch(newObj.getIndex(), 0);
		return returnIndex(newObj, idx);
	}
	
	private int finalRemove(Indexed newObj, int idx)
	{
		//removeLog += "\n testing index " + list.get(idx).getIndex() + " against " + newObj.getIndex();
		if(idx >= list.size())
			return -1;
		if(list.get(idx).getIndex() == newObj.getIndex())
		{
			if(allowDuplicates)
			{
				int df = duplicateFind(newObj, idx);
				if(df != -1)
				{
					list.remove(df);
					return df;
				}
			}
			else
			{
				list.remove(idx);
				return idx;
			}
		}
		return -1;
	}
	
	public int remove(Indexed newObj)
	{
		//removeLog = "";
		int idx = binSearch(newObj.getIndex(), 0, list.size() - 1);
		//removeLog += "idx=" + idx + " out of " + list.size();
		return finalRemove(newObj, idx);
	}
	
	public int removeExp(Indexed newObj)
	{
		int idx = expSearch(newObj.getIndex(), 0);
		return finalRemove(newObj, idx);
	}
	
	public Indexed removeItemWithIndex(long index)
	{
		int loc = getItemLocWithIndexBin(index, 0);
		if(loc >= 0)
			return list.remove(loc);
		else
			return null;
	}
	
	public void removeIndexRange(long startIndex, long endIndex)
	{
		int idxS = getItemLocWithIndexBin(startIndex, 0);
		if(idxS < 0)
			idxS = (idxS + 1) * -1;
		
		int idxE = getItemLocWithIndexBin(endIndex, idxS);
		if(idxE < 0)
			idxE = (idxE + 1) * -1;
		
		list.subList(idxS, idxE).clear();
	}
		
		
	
	
	private void populate(IndexedList otherList)
	{
		list = new ArrayList<Indexed>(otherList.size());
		list.addAll(otherList.getList());
	}
	
	private int expSearch(long index, int startIndex)
	{
		switch(list.size() - startIndex)
		{
		case 0:
			return startIndex;
		case 1:
			if(list.get(startIndex).getIndex() >= index)
				return startIndex;
			else
				return 1 + startIndex;
		default:
			int start = 0;
			int end = 1;
			while(index > list.get(end + startIndex).getIndex())
			{
				start = end + 1;
				end *= 2;
				if(end + startIndex >= list.size())
				{
					end = list.size() - 1 - startIndex;
					break;
				}
			}
			int loc = binSearch(index, start + startIndex, end + startIndex);
			return loc;
		}
	}
	
	private int binSearch(long index, int start, int end)
	{
		long forValue = index;
		int mid = 0;
		long currVal = 0;
		while(start <= end)
		{
			mid = (start + end) >> 1;
			currVal = list.get(mid).getIndex();
			if(currVal == forValue)
				return mid;
			if(currVal > forValue)
				end = mid - 1;
			else
				start = mid + 1;
		}
		return start;  //start at this point is the first value after the one you did not find
	}
	
	public Indexed get(int i)
	{
		if(list.size() <= i)
			return null;
		return list.get(i);
	}
	
	public Indexed getLast()
	{
		if(list.size() == 0)
			return null;
		return list.get(list.size() - 1);
	}
	
	public Indexed findItemWithIndex(long index)
	{
		int idx = binSearch(index, 0, list.size() - 1);
		if(idx >= list.size())
			return null;
		Indexed iObj = list.get(idx);
		if(iObj.getIndex() == index)
			return iObj;
		else
			return null;
	}
	
	/*public Indexed[] findItemsBetween(long index1, long index2)
	{
		int i1 = binSearch(index1, 0, list.size() - 1);
		int i2 =
	}*/
	
	public Indexed findEarlyItemWithIndex(long index)
	{
		int idx = expSearch(index, 0);
		if(idx >= list.size())
			return null;
		Indexed iObj = list.get(idx);
		if(iObj.getIndex() == index)
			return iObj;
		else
			return null;
	}
	
	public Indexed findItemWithIndex(long index, int findAfterLoc)
	{
		int idx = expSearch(index, findAfterLoc);
		if(idx >= list.size())
			return null;
		Indexed iObj = list.get(idx);
		if(iObj.getIndex() == index)
			return iObj;
		else
			return null;
	}
	
	/* getItemLocWithIndexBin
	 * parameters: index - the index to find;  findAfterLoc - the index to begin searching after
	 * returns: the index of the found element, or a negative number equal to the index of the last element
	 * found before the given index - 1
	 */
	public int getItemLocWithIndexBin(long index, int findAfterLoc)
	{
		int idx = binSearch(index, findAfterLoc, list.size() - 1);
		if(idx >= list.size())
			return -1 * idx - 1;
		Indexed iObj = list.get(idx);
		if(iObj.getIndex() == index)
			return idx;
		else
			return -1 * idx - 1;
	}
	
	public int getItemLocWithIndexExp(long index, int findAfterLoc)
	{
		//System.out.println("Finding " + index + " beginning at loc " + findAfterLoc);
		int idx = expSearch(index, findAfterLoc);
		//System.out.println("Found at " + idx);
		if(idx >= list.size())
			return -1 * idx - 1;
		Indexed iObj = list.get(idx);
		if(iObj.getIndex() == index)
			return idx;
		else
			return -1 * idx - 1;
	}
	
	private ArrayList<Integer> intersection(IndexedList otherList)
	{
		ArrayList<Integer> rtnVal = new ArrayList<Integer>();
		int thisLoc = 0;
		int otherLoc = 0;
		long i1, i2;
		//int searchValue;// = Math.min(this.get(0).getIndex(), otherList.get(0).getIndex());
		//IndexedList searchList;
		i1 = this.get(thisLoc).getIndex();
		i2 = otherList.get(otherLoc).getIndex();
		while(true)
		{
			if(i1 > i2)
			{
				otherLoc = otherList.getItemLocWithIndexExp(i1, otherLoc);
				if(otherLoc < 0)  //not found
				{
					otherLoc = otherLoc * -1 - 1;
					if(otherLoc >= otherList.size())
						break;
					i2 = otherList.get(otherLoc).getIndex();
				}
				else //found
				{
					rtnVal.add(thisLoc);
					thisLoc++;
					otherLoc++;
					if(thisLoc >= this.size() || otherLoc >= otherList.size())
						break;
					i1 = this.get(thisLoc).getIndex();
					i2 = otherList.get(otherLoc).getIndex();
				}
			}
			else
			{
				thisLoc = getItemLocWithIndexExp(i2, thisLoc);
				if(thisLoc < 0)
				{
					thisLoc = thisLoc * -1 - 1;
					if(thisLoc >= this.size())
						break;
					i1 = this.get(thisLoc).getIndex();
				}
				else
				{
					rtnVal.add(thisLoc);
					thisLoc++;
					otherLoc++;
					if(thisLoc >= this.size() || otherLoc >= otherList.size())
						break;
					i1 = this.get(thisLoc).getIndex();
					i2 = otherList.get(otherLoc).getIndex();
				}
			}
		}
		return rtnVal;
	}
	
	public ArrayList<Indexed> getNotInList2(IndexedList otherList)
	{
		ArrayList<Integer> intIndeces = intersection(otherList);
		ArrayList<Indexed> rtnVal = new ArrayList<Indexed>(this.size());
		rtnVal.addAll(this.getList());
		for(int i = intIndeces.size() - 1; i >= 0; i--)
			rtnVal.remove((int) intIndeces.get(i));
		return rtnVal;
	}
	
	public ArrayList<Indexed> getInList2(IndexedList otherList)
	{
		ArrayList<Integer> intIndeces = intersection(otherList);
		ArrayList<Indexed> rtnVal = new ArrayList<Indexed>(intIndeces.size());
		for(int i = 0; i < intIndeces.size(); i++)
			rtnVal.add(this.get(intIndeces.get(i)));
		return rtnVal;
	}
	
	public void removeAllInList(IndexedList otherList)
	{
		int otherListLoc = 0;
		int i = 0;
		while(i < list.size())
		{
			long findIndex = list.get(i).getIndex();
			int otherLoc = otherList.getItemLocWithIndexExp(findIndex, otherListLoc);
			if(otherLoc >= 0)
			{
				//System.out.println("Item " + findIndex + " found; otherLoc=" + otherLoc);
				list.remove(i);
				i--;
				otherListLoc = otherLoc;
			}
			else
				otherListLoc = otherLoc * -1 - 1;
			i++;
			if(otherListLoc >= otherList.size())
				break;
		}
	}
	
	public ArrayList<Indexed> getNotInList(IndexedList otherList)
	{
		ArrayList<Indexed> rtnVal = new ArrayList<Indexed>();
		int otherListLoc = 0;
		int i = 0;
		while(i < list.size())
		{
			long findIndex = list.get(i).getIndex();
			int otherLoc = otherList.getItemLocWithIndexExp(findIndex, otherListLoc);
			if(otherLoc < 0)
			{
				//System.out.println("Item " + findIndex + " not found; otherLoc=" + otherLoc);
				rtnVal.add(list.get(i));
				otherListLoc = otherLoc * -1 - 1;
			}
			else
				otherListLoc = otherLoc;
			i++;
			if(otherListLoc >= otherList.size())
				break;
		}
		long lastValue = otherList.getLast().getIndex();
		while(i < list.size())
		{
			if(list.get(i).getIndex() > lastValue)
				break;
			i++;
		}
		while(i < list.size())
		{
			rtnVal.add(list.get(i));
			i++;
		}
		return rtnVal;
	}
	
	public ArrayList<Indexed> getInList(IndexedList otherList)
	{
		ArrayList<Indexed> rtnVal = new ArrayList<Indexed>();
		int otherListLoc = 0;
		int i = 0;
		while(i < list.size())
		{
			long findIndex = list.get(i).getIndex();
			int otherLoc = otherList.getItemLocWithIndexExp(findIndex, otherListLoc);
			if(otherLoc >= 0)
			{
				//System.out.println("Item " + findIndex + " found; otherLoc=" + otherLoc);
				rtnVal.add(list.get(i));
				otherListLoc = otherLoc;
			}
			else
				otherListLoc = otherLoc * -1 - 1;
			i++;
			if(otherListLoc >= otherList.size())
				break;
		}
		return rtnVal;
	}
	
	public int getSize()
	{
		return list.size();
	}
	
	
	
	public static void intersectionTest()
	{
		int[] testCase1 = {2,6,8,9,11,12,15,16,17,18,21,25,26,30,32,34,35,36,37,38};
		int[] testCase2 = {0,1,2,4,5,6,7,11,17,18,19,20,21,22,23,29,31,32,33,34,36};
		
		IndexedList il = new IndexedList();
		il.clear();
		for(int i = 0; i < testCase1.length; i++)
			il.add(new IndexedInteger(testCase1[i]));
		
		IndexedList il2 = new IndexedList();
		il2.clear();
		for(int i = 0; i < testCase2.length; i++)
			il2.add(new IndexedInteger(testCase2[i]));
		
		int[] expected = {2,6,11,17,18,21,32,34,36};
		ArrayList<Indexed> output = il.getInList2(il2);
		if(output.size() != expected.length)
			System.out.println("Error - output size is " + output.size() + " when it should be " + expected.length);
		for(int i = 0; i < expected.length; i++)
		{
			boolean found = false;
			for(int j = 0; j < output.size(); j++)
			{
				if(output.get(j).getIndex() == expected[i])
					found = true;
			}
			if(!found)
				System.out.println("Error - didn't find " + expected[i]);
		}
	}
	
	public static void indexedListTest(int listSize, int nCases)
	{
		int listSize2 = 30;
		System.out.println("NCases = " + nCases);
		long time2 = 0;
		for(int k = 0; k < nCases; k++)
		{
		int[] testCase1 = null; //{2,6,8,9,11,12,15,16,17,18,21,25,26,30,32,34,35,36,37,38};
		int[] testCase2 = null;//{0,1,2,4,5,6,7,11,17,18,19,20,21,22,23,29,31,32,33,34,36};
		//int[] testCase1 = {1,1,1};
		//int[] testCase2 = {1,1,1};
		IndexedList il = new IndexedList();
		for(int i = 0; i < listSize; i++)
		{
			IndexedInteger ii = new IndexedInteger((int) (UWNHRando.rand() * 40));
			il.add(ii);
		}
		if(testCase1 != null)
		{
			il.clear();
			for(int i = 0; i < testCase1.length; i++)
				il.add(new IndexedInteger(testCase1[i]));
		}
		/*System.out.print("List 1:");
		for(int i = 0; i < il.size(); i++)
			System.out.print(il.get(i).getIndex() + "  ");
		System.out.println();*/
		
		IndexedList il2 = new IndexedList();
		for(int i = 0; i < listSize2; i++)
		{
			IndexedInteger ii = new IndexedInteger((int) (UWNHRando.rand() * 40));
			il2.add(ii);
		}
		if(testCase2 != null)
		{
			il2.clear();
			for(int i = 0; i < testCase2.length; i++)
				il2.add(new IndexedInteger(testCase2[i]));
		}
		/*System.out.print("List 2:");
		for(int i = 0; i < il2.size(); i++)
			System.out.print(il2.get(i).getIndex() + "  ");
		System.out.println();*/
		long time = System.nanoTime();
		ArrayList<Indexed> inList = il.getInList(il2);
		time = System.nanoTime() - time;
		ArrayList<Indexed> notInList1 = il.getNotInList(il2);
		
		ArrayList<Indexed> notInList2 = il2.getNotInList(il);
		
		time2 += time;
		for(int i = 0; i < il.size(); i++)
		{
			long val = il.get(i).getIndex();
			boolean found = false;
			for(int j = 0; j < il2.size(); j++)
			{
				if(il2.get(j).getIndex() == val)
				{
					found = true;
					break;
				}
			}
			boolean found2 = false;
			for(int j = 0; j < inList.size(); j++)
			{
				if(inList.get(j).getIndex() == val)
				{
					found2 = true;
					break;
				}
			}
			if(found && !found2)
				System.out.println("getInList() is missing a value");
			if(!found && found2)
				System.out.println("getInList() has an extra value");
			boolean found3 = false;
			for(int j = 0; j < notInList1.size(); j++)
			{
				if(notInList1.get(j).getIndex() == val)
				{
					found3 = true;
					break;
				}
			}
			if(found3 && found)
				System.out.println("l1.getNotInList(l2) has an extra value");
			if(!found2 && !found3)
				System.out.println("l1.getNotInList(l2) is missing a value");
		}
		/*System.out.print("In both lists:");
		for(int i = 0; i < inList.size(); i++)
			System.out.print(inList.get(i).getIndex() + "  ");
		System.out.println();*/
		
		
		/*System.out.print("Not in list 2:");
		for(int i = 0; i < notInList1.size(); i++)
			System.out.print(notInList1.get(i).getIndex() + "  ");
		System.out.println();*/
		
		for(int i = 0; i < il2.size(); i++)
		{
			long val = il2.get(i).getIndex();
			boolean found = false;
			for(int j = 0; j < il.size(); j++)
			{
				if(il.get(j).getIndex() == val)
				{
					found = true;
					break;
				}
			}
			boolean found3 = false;
			for(int j = 0; j < notInList2.size(); j++)
			{
				if(notInList2.get(j).getIndex() == val)
				{
					found3 = true;
					break;
				}
			}
			if(found && found3)
				System.out.println("l2.getNotInList(l1) has an extra value");
			if(!found && !found3)
				System.out.println("l2.getNotInList(l1) is missing a value");
		}
		
		/*System.out.print("Not in list 1:");
		for(int i = 0; i < notInList2.size(); i++)
			System.out.print(notInList2.get(i).getIndex() + "  ");
		System.out.println();*/
	}
		
		System.out.println(time2 + " ns");
	}
	
}


class FlagPanel extends JPanel implements ActionListener, ChangeListener
{
	String[] flags;
	JCheckBox[] checks;
	boolean multiSelect;
	boolean bitField;
	boolean slider;
	String hybridFlag, hybridFlag2;
	RandomizerWindow rw;
	int maxVal;
	int minVal;
	JSlider slide;
	JLabel slideDesc;
	String slideText;
	private boolean ignoreSliderChange;
	
	FlagPanel(String title, String[] options, String[] flags, boolean multiSelect, boolean bitField, boolean slider, RandomizerWindow rw)
	{
		setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		
		this.rw = rw;
		this.flags = flags;
		if(slider)
		{
			this.slider = true;
			slideText = options[0];
			slideDesc = new JLabel(options[0]);
			checks = new JCheckBox[0];
			//setLayout(new GridLayout(1,1));
			return;
		}
		setLayout(new GridLayout(options.length, 1));
		checks = new JCheckBox[options.length];
		for(int i = 0; i < options.length; i++)
		{
			checks[i] = new JCheckBox(options[i]);
			checks[i].addActionListener(this);
			add(checks[i]);
		}
		if(bitField)
		{
			this.bitField = true;
			hybridFlag = flags[0].charAt(0) + "0";
		}
		else
			this.bitField = false;
		this.multiSelect = multiSelect;
		
		maxVal = 9;
	}
	
	public void setMinVal(int v)
	{
		minVal = v;
	}
	
	public void setMaxVal(int v)
	{
		maxVal = v;
		if(slider)
			initSlider();
		//if(v > 9)
			//hybridFlag2 = flags[4].charAt(0) + "0";
	}
	
	public void setValue(int v)
	{
		//ignoreSliderChange = true;
		if(v >= minVal && v <= maxVal)
		{
			slide.setValue(v);
			//int val = slide.getValue();
			//slideDesc.setText(slideText + " " + val);
			//hybridFlag = flags[0].charAt(0) + String.valueOf(val);
		}
		//ignoreSliderChange = false;
	}
	
	private void initSlider()
	{
		slide = new JSlider(JSlider.HORIZONTAL, minVal, maxVal, minVal);
		slide.addChangeListener(this);
		
		//Turn on labels at major tick marks.
		int mts = 5;
		if(maxVal == 8)
			mts = 6;
		slide.setMajorTickSpacing(mts);
		slide.setMinorTickSpacing(1);
		slide.setPaintTicks(true);
		slide.setPaintLabels(true);
		
		slideDesc.setText(slideText + " " + minVal);
		JPanel pan = new JPanel(new GridLayout(4,1));
		pan.add(new JPanel());
		pan.add(slideDesc);
		pan.add(slide);
		pan.add(new JPanel());
		hybridFlag = flags[0].charAt(0) + String.valueOf(minVal);
		//invalidate();
		setLayout(new BorderLayout());
		this.add(pan, BorderLayout.CENTER);
		//revalidate();
	}
	
	

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		int i = 0;
		for(; i < checks.length; i++)
			if(e.getSource() == checks[i])
				break;
		if(bitField)
		{
			rw.clearFlag(hybridFlag);
			if(maxVal >  9) //the r flag
				rw.clearFlag(hybridFlag2);
			int hv = Integer.parseInt(String.valueOf(hybridFlag.charAt(1)));
			if(maxVal >  9) //the r flag
				hv += Integer.parseInt(String.valueOf(hybridFlag2.charAt(1))) << 4;
			int n = -1;
			//int n2 = -1;
			if(flags[i].charAt(0) == hybridFlag.charAt(0))
				n = Integer.parseInt(String.valueOf(flags[i].charAt(1)));
			else
				n = Integer.parseInt(String.valueOf(flags[i].charAt(1))) << 4;
			if(checks[i].isSelected())
			{
				if(flags[0].charAt(1) == '0' && !checks[0].isSelected())
					checks[0].setSelected(true);
				hv += n;
				int j = checks.length;
				while(hv > maxVal)  //if you overselect then unselect down to include n
				{
					j--;
					if(i == j)
						continue;
					//if(j == -1)
						//System.out.println("aaah");
					if(checks[j].isSelected())
					{
						checks[j].setSelected(false);
						hv -= Integer.parseInt(String.valueOf(flags[j].charAt(1)));
					}
				}
			}
			else
			{
				hv -= n;
				if(n == 0)  //if you delesect the 0 flag
				{
					hybridFlag = flags[0].charAt(0) + "0";
					if(hybridFlag2.length() > 0)
						hybridFlag2 = flags[4].charAt(0) + "0";
					for(int j = 0; j < checks.length; j++)
						checks[j].setSelected(false);
					return;
				}
				if(hv == 0 && !flags[0].endsWith("0"))  //if you set the value to 0 and there is no 0 flag
				{
					hybridFlag = String.valueOf(hybridFlag.charAt(0)) + hv;
					return;
				}
			}
			hybridFlag = String.valueOf(hybridFlag.charAt(0)) + (hv & 15);
			rw.setFlag(hybridFlag);
			if(maxVal > 9)
			{
				if(hv > 9)
				{
					hybridFlag2 = String.valueOf(hybridFlag2.charAt(0)) + (hv >> 4);
					rw.setFlag(hybridFlag2);
				}
				else
					hybridFlag2 = hybridFlag2.charAt(0) + "0";
			}
		}
		else
		{
			if(checks[i].isSelected())
			{
				rw.setFlag(flags[i]);
				/*if(flags[i].equals("Z"))  //this flag changes the text of 
				{
					rw.setSmallMapSize(true);
				}*/
				if(!multiSelect)
				{
					for(int j = 0; j < checks.length; j++)
					{
						if(j == i)
							continue;
						if(checks[j].isSelected())
						{
							checks[j].setSelected(false);
							rw.clearFlag(flags[j]);
						}
					}
				}
			}
			else
			{
				rw.clearFlag(flags[i]);
				/*if(flags[i].equals("Z"))
					rw.setSmallMapSize(false);*/
			}
		}
	}

	public void preSelectFlags(String initflags) 
	{
		for(int i = 0; i < flags.length; i++)
		{
			if(initflags.indexOf(flags[i]) > 0)
				checks[i].setSelected(true);
		}
	}

	@Override
	public void stateChanged(ChangeEvent ev) 
	{
		if(ignoreSliderChange)
			return;
		// TODO Auto-generated method stub
		int val = slide.getValue();
		slideDesc.setText(slideText + " " + val);
		//also change the flag
		rw.clearFlag(hybridFlag);
		hybridFlag = flags[0].charAt(0) + String.valueOf(val);
		rw.setFlag(hybridFlag);
	}
}

class EditPanel extends JPanel implements ActionListener
{
	String[] opts;
	String[] vals;
	String[][] avail;
	Object[] all;
	RandomizerWindow rw;
	int[] listI;
	int ti;
	int listOffset;
	//JTextField[] allText;
	
	EditPanel(int tabIdx, String[] options, String[][] availOptions, boolean batch, RandomizerWindow rwin, int[] listSel)
	{
		ti = tabIdx;
		opts = options;
		avail = availOptions;
		all = new Object[opts.length];
		rw = rwin;
		listI = listSel;
		listOffset = 0;
		//allText = new JTextField[opts.length];
		vals = new String[options.length];
		
		setLayout(new GridLayout(6, 1));
		if(opts.length == 1)   //just name
		{
			String val = rw.theme[ti][0][listI[0]];
			for(int i = 0; i < 2; i++)
				add(new JPanel());
			add(makeStdPanel(0, opts[0], val));
			for(int i = 0; i < 3; i++)
				add(new JPanel());
			return;
		}
		int i  = 0;
		listOffset = 1;  //list offset is 1 b/c list[0] is all
		if(batch)
			i++;
		if(listI[0] == 0)
			listOffset = 0;
		for(; i < opts.length; i++)
		{
			String val = rw.theme[ti][i][listI[0] - listOffset];
			if(availOptions[i + 1].length == 0)
			{
				add(makeStdPanel(i, opts[i], val));
			}
			else
			{
				add(makeSelPanel(i, opts[i], avail[i + 1], val));
			}
		}
	}
	
	private JPanel makeStdPanel(int index, String opt, String val)
	{
		JPanel pan = new JPanel();
		pan.add(new JLabel(opt));
		JTextField jtf = new JTextField(20);
		jtf.setText(val);
		pan.add(jtf);
		jtf.addActionListener(this);
		all[index] = jtf;
		return pan;
	}
	
	private JPanel makeSelPanel(int index, String opt, String[] choices, String val)
	{
		JPanel pan = new JPanel();
		pan.add(new JLabel(opt));
		JComboBox jcb = new JComboBox(choices);
		jcb.setSelectedItem(val);
		jcb.addActionListener(this);
		pan.add(jcb);
		all[index] = jcb;
		return pan;
	}

	@Override
	public void actionPerformed(ActionEvent ev) 
	{
		for(int i = 0; i < all.length; i++)
		{
			if(ev.getSource() == all[i])
			{
				if(ev.getSource() instanceof JComboBox)
				{
					JComboBox bx = (JComboBox) all[i];
					vals[i] = (String) bx.getSelectedItem();
				}
				else
				{
					JTextField tf = (JTextField) all[i];
					vals[i] = (String) tf.getText();
				}
				if(listI[0] == 0)
				{
					for(int l = 0; l < rw.theme[ti][0].length; l++)
					{
						rw.theme[ti][i][l] = vals[i];
					}
				}
				else
				{
					for(int l = 0; l < listI.length; l++)
					{
						rw.theme[ti][i][listI[l] - listOffset] = vals[i];
					}
				}
				break;
			}
		}
	}

	public void setSelIndices(int[] selectedIndices) 
	{
		listI = selectedIndices;
		/*for(int i = 0; i < all.length; i++)
		{
			String val = rw.theme[ti][i][listI[0]];
			if(all[i] instanceof JComboBox)
			{
				JComboBox bx = (JComboBox) all[i];
				bx.setSelectedItem(val);
			}
			else
			{
				JTextField tf = (JTextField) all[i];
				tf.setText(val);
			}
		}*/
	}
}



class ListPanel extends JPanel implements ActionListener, ListSelectionListener
{
	JButton bEdit;
	JList list;
	JScrollPane listPane;
	EditPanel ePan;
	RandomizerWindow rw;
	String mTag;
	String[] mList;
	String[] mEOpts;
	String[][] mAvail;
	int index;
	
	ListPanel(String tag, int tabIndex, String[] listItems, String[] editOptions, String[][] avail, RandomizerWindow rwin)
	{
		rw = rwin;
		mTag = tag;
		mList = listItems;
		mEOpts = editOptions;
		mAvail = avail;
		index = tabIndex;
		list = new JList(listItems); //data has type Object[]
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		list.addListSelectionListener(this);
		
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(250, 80));
		
		bEdit = new JButton("Edit All...");
		bEdit.addActionListener(this);
		boolean batch = false;
		if(avail.length > 0)
		{
			batch = true;
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
		//ePan = new EditPanel(index, editOptions, avail, batch, rw, list.getSelectedIndices());
		
		setLayout(new BorderLayout());
		add(bEdit, BorderLayout.NORTH);
		add(listScroller, BorderLayout.WEST);
		//add(ePan, BorderLayout.CENTER);
		
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) 
	{
		//This handles the list being changed
		if(list.getSelectedIndices().length > 1)
		{
			invalidate();
			if(ePan != null)
				remove(ePan);
			ePan = new EditPanel(index, mEOpts, mAvail, true, rw, list.getSelectedIndices());
			add(ePan, BorderLayout.CENTER);
			revalidate();
		}
		else
		{
			invalidate();
			if(ePan != null)
				remove(ePan);
			ePan = new EditPanel(index, mEOpts, mAvail, false, rw, list.getSelectedIndices());
			add(ePan, BorderLayout.CENTER);
			revalidate();
		}
		//ePan.setSelIndices(list.getSelectedIndices());	
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		//The batch edit button
		BatchEditWindow biw = new BatchEditWindow(mTag, index, mList, mEOpts, mAvail, rw);
	}
}

class BatchEditWindow extends JFrame //implements ActionListener
{
	JTable mainTable;
	//JButton save;
	JScrollPane tableScr;
	RandomizerWindow rw;
	int index;
	
	class BEComboBox extends JComboBox implements TableCellRenderer
	{
		
		BEComboBox(String[] choices)
		{
			super(choices);
		}

		@Override
		public Component getTableCellRendererComponent( 
				JTable table, Object color,
                boolean isSelected, boolean hasFocus,
                int row, int column) 
		{
			return this;
		}
		
	}
	
	class BETableModel extends AbstractTableModel
	{
		String mid;
		String[] mListItems;
		String[] mEditOptions;
		String[][] mAvail;
		String[][] rawData;
		boolean allRow;
		
		BETableModel(String id, String[] listItems, String[] editOptions, String[][] avail, String[][] tableData)
		{
			mid = id;
			mListItems = listItems;
			mEditOptions = editOptions;
			mAvail = avail;
			rawData = new String[editOptions.length + 1][listItems.length + 1];
			rawData[0][0] = mid;
			for(int i = 0; i < editOptions.length; i++)
				rawData[i + 1][0] = editOptions[i];
			for(int i = 0; i < listItems.length; i++)
				rawData[0][i + 1] = listItems[i];
			allRow = false;
			if(listItems[0].equals("All Ports"))
				allRow = true;
			populate(tableData);
		}
		
		public void populate(String[][] data)
		{
			int a = 0;
			if(allRow)
				a = 1;
			for(int i = 0; i < data.length; i++)
			{
				for(int j = 0; j < data[0].length; j++)
				{
					if(mAvail[i + 1].length > 1)
					{
						for(int k = 0; k < mAvail[i + 1].length; k++)
						{
							if(mAvail[i + 1][k].equals(data[i][j]))
							{
								rawData[i + 1][j + 1 + a] = mAvail[i + 1][k];
								break;
							}
						}
					}
					else
					{
						rawData[i + 1][j + 1 + a] = data[i][j];
					}
				}
			}
		}
		
		@Override
		public int getColumnCount() 
		{
			// TODO Auto-generated method stub
			return mEditOptions.length + 1;
		}

		@Override
		public int getRowCount() 
		{
			// TODO Auto-generated method stub
			return mListItems.length + 1;
		}

		@Override
		public Object getValueAt(int row, int col) 
		{
			// TODO Auto-generated method stub
			return rawData[col][row];
		}
		
		public boolean isCellEditable(int row, int col)
		{
			if(row == 0 || col == 0)
				return false;
			return true;
		}
		
		public void setValueAt(Object value, int row, int col) 
		{
			if(allRow && row == 1 && col > 1)
			{
				for(int r = 2; r < rawData[0].length; r++)
				{
					rawData[col][r] = (String) value;
					rw.theme[index][col - 1][r - 2] = (String) value;
				}
				//populateData();
			}
			else
			{
				rawData[col][row] = (String) value;
				rw.theme[index][col - 1][row - 1] = (String) value;
				
			}
			fireTableCellUpdated(row, col);
	    } 
		
	}
	
	/*class BETable extends JTable
	{
		String[][] mAvail;
		
		BETable(String id, String[] listItems, String[] editOptions, String[][] avail)
		{
			super(new BETableModel(id, listItems, editOptions, avail));
			mAvail = avail;
		}
		
		public TableCellRenderer getCellRenderer(int row, int column) 
		{
	        if (mAvail[column].length > 1 && row > 0) 
	        {
	            return new BEComboBox(mAvail[column]);
	        }
	       
	        return super.getCellRenderer(row, column);
	    }
	}*/
	
	BatchEditWindow(String id, int tabIndex, String[] listItems, String[] editOptions, String[][] avail, RandomizerWindow rwin)
	{
		//editOptions is columns
		rw = rwin;
		index = tabIndex;
		//index = id;
		
		mainTable = new JTable(new BETableModel(id, listItems, editOptions, avail, rw.theme[tabIndex]));
		
		tableScr = new JScrollPane(mainTable);
		mainTable.setFillsViewportHeight(true);
		
		for(int i = 0; i < avail.length; i++)
		{
			if(avail[i].length > 0)
			{
				TableColumn tc = mainTable.getColumnModel().getColumn(i);
				tc.setCellEditor(new DefaultCellEditor(new BEComboBox(avail[i])));
			}
		}
		
		/*save = new JButton("Save Changes");
		save.addActionListener(this);*/
		
		getContentPane().add(tableScr, BorderLayout.CENTER);
		//getContentPane().add(save, BorderLayout.SOUTH);
		
		setSize(800,600);
		setLocation(150,150);
		setVisible(true);
		
	}

	/*@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		// The save button
		
	}*/
	
	
	
}

class InputLinePanel extends JPanel implements ActionListener, DocumentListener
{
	JLabel lbl;
	JTextField txt;
	JButton btn;
	JButton btn2;
	RandomizerWindow rw;
	
	InputLinePanel(String labelText, String defaultText, String buttonText, String b2Text, RandomizerWindow rw, boolean fxSaveFile)
	{
		lbl = new JLabel(labelText);
		txt = new JTextField(defaultText, 25);
		//txt.setColumns(25);
		btn = new JButton(buttonText);
		this.rw = rw;
		
		setLayout(new FlowLayout());
		add(lbl);
		add(txt);
		add(btn);
		btn.addActionListener(this);
		if(b2Text != null)
		{
			btn2 = new JButton(b2Text);
			add(btn2);
			btn2.addActionListener(this);
		}
		if(fxSaveFile)
			txt.getDocument().addDocumentListener(this);
	}
	
	public void setTextText(String s)
	{
		txt.setText(s);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == btn)
			rw.execute(this, 0);
		else
			rw.execute(this, 1);
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		// TODO Auto-generated method stub
		rw.updateSaveName();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		// TODO Auto-generated method stub
		rw.updateSaveName();
	}
}


class RandomizerWindow extends JFrame implements ActionListener
{
	InputLinePanel[] inputLines;
	JPanel topPane;
	JButton[] tournButtons;
	boolean threadRunning;
	RandomizerThread rt;
	Timer tim;
	JFileChooser jfc;
	JTabbedPane jtp;
	
	JLabel infoLabel;
	
	ArrayList<FlagPanel> opts;
	
	String[][][] theme;
	String[][][] origTheme;
	private String[] themeTables;
	public String[][] portTables;
	
	boolean romsToOrigROMDir;
	JCheckBox bx1, bx2;
	
	
	class RandomizerThread extends Thread
	{
		String failure;
		
		RandomizerThread()
		{
			
		}
		
		public void run()
		{
			SNESRom rom = null;
			SNESRom nr = null;
			String flags = inputLines[2].txt.getText();
			String dir = System.getProperty("user.dir") + File.separator;
			String outFile = dir + inputLines[3].txt.getText();
			//String outFile2 = dir + "2" + inputLines[3].txt.getText();
			String fname = inputLines[1].txt.getText();
			String[] error = {""};
			try
			{
				rom = new SNESRom(fname);
			}
			catch(Exception ex)
			{
				tim.stop();
				failure += fname + "\n was not found\n";
				JOptionPane.showMessageDialog(RandomizerWindow.this, failure, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			try
			{
				//nr = new SNESRom(fname);
				
				nr = rom.randomize(flags, outFile, error, romsToOrigROMDir, theme);
				rom = new SNESRom(fname);
				DiffTool dt = new DiffTool(nr, rom);
				dt.report(nr.homeDir + File.separator + "diffreport.txt");
				outFile = nr.filename;
				/*int mode = 0;
				int flagi = flags.indexOf("r");
				if(flagi > -1)
				{
					int x = Integer.parseInt(String.valueOf(flags.charAt(flagi + 1)));
					mode = x * 16;
				}
				
				flagi = flags.indexOf("L");
				if(flagi > -1)
				{
					int x = Integer.parseInt(String.valueOf(flags.charAt(flagi + 1)));
					if((x & 2) == 2)
						mode |= 32;
				}*/
				/*String output = nr.getFinalSearchLocData(mode);
				System.out.println(output);
				if(flags.contains("x"))
					saveSpoiler(dir, inputLines[0].txt.getText(), output);*/
				/*long ll = Long.parseLong(inputLines[0].txt.getText());
				UltimaRando.setSeed(ll);
				rom = new NESRom(fname);
				NESRom nr2 = rom.randomize(flags, outFile2);*/
				
			}
			catch(Exception ex)
			{
				tim.stop();
				System.err.println(ex.getMessage());
				ex.printStackTrace();
				failure += "This seed failed to successfully generate a ROM.\nFailure=" + error[0] + "\nSee makelog for details.\n";
				JOptionPane.showMessageDialog(RandomizerWindow.this, failure, "Sorry", JOptionPane.ERROR_MESSAGE);
				return;
			}
			//
			tim.stop();
			saveConfig();
			
			rt = null;
			//System.out.println(nr.getFinalSearchLocData());
			/*System.out.println(nr.getODLocations());
			MapWindow mw = null;
			if(nr.gameMap != null)
				mw = new MapWindow(nr.gameMap, true);
			else
				mw = new MapWindow(UltimaRando.combineMap(nr.getPotMap()), true);
			mw.setPOIList(nr.getPOIList());
			mw.setMoongates(nr.getMoongates());*/
			//System.out.println(nr.getInitCharData());
			JOptionPane.showMessageDialog(RandomizerWindow.this, outFile + "\nhas been successfully generated.", "Done", JOptionPane.INFORMATION_MESSAGE);
		}

		private void saveSpoiler(String dir, String seed, String output) 
		{
			File f = new File(dir + "UWNHSpoiler." + seed + ".txt");
			try
			{
				if(!f.exists())
					f.createNewFile();
				FileWriter fw = new FileWriter(f);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(output);
				bw.close();
			}
			catch(Exception ex)
			{
				System.out.println(ex.getMessage());
			}
			
		}
	}
	
	RandomizerWindow()
	{
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ex)
		{
			System.err.println(ex.getMessage());
		}
		
		bx1 = new JCheckBox("Original ROM");
		bx2 = new JCheckBox("Randomizer");
		
		inputLines = setupInputLines();
		loadConfig();
		loadOrigTheme();
		
		
		JPanel topPane1 = new JPanel(new GridLayout(2,2));
		topPane1.add(inputLines[0]);
		topPane1.add(inputLines[1]);
		//JPanel topPane2 = new JPanel(new GridLayout(1,2));
		topPane1.add(inputLines[2]);
		topPane1.add(inputLines[3]);
		
		JPanel dirPane = new JPanel(new GridLayout(2,2));
		JLabel lbl1 = new JLabel("Output ROMs to");
		JLabel lbl2 = new JLabel("Directory of:");
		
		dirPane.add(lbl1);
		dirPane.add(bx1);
		dirPane.add(lbl2);
		dirPane.add(bx2);
		bx1.addActionListener(this);
		bx2.addActionListener(this);
		
		if(origTheme == null)  //because there was no config file to load
		{
			bx1.setSelected(true);
			this.romsToOrigROMDir = true;
		}
		
		JPanel pane3 = new JPanel(new BorderLayout());
		pane3.add(inputLines[4], BorderLayout.CENTER);
		pane3.add(dirPane, BorderLayout.EAST);
		
		topPane = new JPanel(new GridLayout(2, 1));
		topPane.add(topPane1);
		//topPane.add(topPane2);
		//topPane.add(inputLines[4]);
		topPane.add(pane3);
		
		if(origTheme != null)
		{
			jtp = new JTabbedPane();
			setupPanels();
			initFlags(inputLines[2].txt.getText());
			
			updateTheme();
		}
		getContentPane().add(topPane, BorderLayout.NORTH);
		if(origTheme != null)
		{
			getContentPane().add(jtp, BorderLayout.CENTER);
		}
		else
		{
			String info = "Data from the original Uncharted Waters: New Horizons ROM \n" +
						  "is required to continue.  Please locate this ROM now.";
			infoLabel = new JLabel(info);
			getContentPane().add(infoLabel, BorderLayout.CENTER);
		}
		setTitle("Uncharted Waters:New Horizons Randomizer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(100, 100);
		setSize(1000, 600);
		//setResizable(false);
		setVisible(true);
		
	}
	
	private void finishUI()
	{
		loadOrigTheme();
		if(origTheme != null)
		{
			jtp = new JTabbedPane();
			setupPanels();
			initFlags(inputLines[2].txt.getText());
			
			updateTheme();
			invalidate();
			getContentPane().remove(infoLabel);
			getContentPane().add(jtp, BorderLayout.CENTER);
			revalidate();
		}
	}
	
	private void loadOrigTheme() 
	{
		String fname = inputLines[1].txt.getText();
		try
		{
			SNESRom rom = new SNESRom(fname);
			origTheme = rom.getOrigThemeData();
			theme = origTheme;
		}
		catch(Exception ex)
		{
			//tim.stop();
			/*failure += fname + "\n was not found\n";
			JOptionPane.showMessageDialog(RandomizerWindow.this, failure, "Error", JOptionPane.ERROR_MESSAGE);*/
			//ex.printStackTrace();
			origTheme = null;
			return;
		}
		
	}

	private void initFlags(String fl) 
	{
		String currFlag = "";
		for(int i = 0; i < fl.length(); i++)
		{
			char ch = fl.charAt(i);
			if(Character.isAlphabetic(ch))
			{
				if(currFlag.length() > 0)
				{
					initFlag(currFlag);
					currFlag = "";
				}
				currFlag += ch;
			}
			else
			{
				currFlag += ch;
				//initFlag(currFlag);
				//currFlag = "";
			}
		}
		if(currFlag.length() > 0)
			initFlag(currFlag);
	}
	
	private void initFlag(String fl)
	{
		char a = fl.charAt(0);
		for(int i = 0; i < opts.size(); i++)
		{
			FlagPanel fp = opts.get(i);
			
			if(fl.length() >= 2)
			{
				boolean found = false;
				for(int k = 0; k < fp.flags.length; k++)
				{
					if(fp.flags[k].charAt(0) == a)
					{
						found = true;
						break;
					}
				}
				if(!found)
					continue;
			}
			if(fp.bitField == true && fl.length() >= 2)
			{
				int val = Integer.parseInt("" + fl.charAt(1));
				for(int j = 0; j < fp.flags.length; j++)
				{
					String s = fp.flags[j];
					if(!s.startsWith(fl.charAt(0) + ""))
						continue;
					int val2 = Integer.parseInt("" + s.charAt(1));
					if((val & val2) > 0 || val2 == 0)
						fp.checks[j].setSelected(true);
					fp.hybridFlag = "" + s.charAt(0) + val;
				}
			}
			else if(fp.slider)
			{
				int val = Integer.parseInt(fl.substring(1));
				fp.setValue(val);
			}
			else
			{
				for(int j = 0; j < fp.flags.length; j++)
				{
					if(fp.flags[j].equals(fl))
					{
						fp.checks[j].setSelected(true);
						//if(a == 'Z')
							//setSmallMapSize(true);
						return;
					}
				}
			}
		}
		
	}
	
	public void clearFlag(String ff) 
	{
		String s = inputLines[2].txt.getText();
		int idx = s.indexOf(ff);
		if(idx > -1)
		{
			String s1 = s.substring(0, idx);
			String s2 = s.substring(idx + ff.length(), s.length());
			inputLines[2].setTextText(s1 + s2);
			updateSaveName();
			if(ff.equals("M"))
				clearMapFlags();
		}
	}
	
	public void setFlag(String ff) 
	{
		String s = inputLines[2].txt.getText();
		char fl = ff.charAt(0);
		if(s.indexOf(fl) == -1)
		{
			s += ff;
		}
		else
		{
			String[] pp = s.split(String.valueOf(fl));
			if(pp[1].length() > 0)
			{
				char ch = pp[1].charAt(0);
				while(Character.isDigit(ch))
				{
					pp[1] = pp[1].substring(1);
					if(pp[1].length() == 0)
						break;
					ch = pp[1].charAt(0);
				}
			}
			s = pp[0] + ff + pp[1];
		}
		inputLines[2].setTextText(s);
		updateSaveName();
		if(fl == 'M')
		{
			setMapFlags();
		}
	}
	
	private void setMapFlags()
	{
		for(int i = 0; i < opts.size(); i++)
		{
			FlagPanel fp = opts.get(i);
			if(fp.flags[0].equals("C") || fp.flags[0].equals("I") || fp.flags[0].equals("O"))
			{
				String hFlag = fp.flags[0] + fp.slide.getValue();
				setFlag(hFlag);
			}
		}
	}
	
	private void clearMapFlags()
	{
		String[] mchars = {"C", "I", "O"};
		for(int i = 0; i < mchars.length; i++)
		{
			String s = inputLines[2].txt.getText();
			int x = s.indexOf(mchars[i]);
			if(x >= 0)
			{
				//String fl = "";
				int y = x + 1;
				while(s.charAt(y) < 'A')  //while s is a number
				{
					y++;
					if(y >= s.length())
						break;
				}
				String fl = s.substring(x, y);
				clearFlag(fl);
			}
		}
	}
	
	private String loadTextFile(String tf)
	{
		String currDir = System.getProperty("user.dir");
		String path = tf;
		if(!tf.contains(File.separator))
			path = currDir + File.separator + tf;
		File f = new File(path);
		String rv = "";
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = br.readLine();
			while(line != null)
			{
				rv += line + "\n";
				line = br.readLine();
			}
			br.close();
			return rv;
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
			return null;
		}
	}
	
	private void loadConfig()
	{
		String s = loadTextFile("UWNHRandoConfig.txt");
		if(s != null)
		{
			String[] a = s.split("\n");
			String rom = a[0].substring(a[0].indexOf("=") + 1);
			inputLines[1].setTextText(rom);
			String fl = a[1].substring(a[1].indexOf("=") + 1);
			inputLines[2].setTextText(fl);
			updateSaveName();
			String fi = a[2].substring(a[2].indexOf("=") + 1);
			this.romsToOrigROMDir = Boolean.parseBoolean(fi);
			boolean b = romsToOrigROMDir;
			bx1.setSelected(b);
			bx2.setSelected(!b);
		}
	}
	
	public void updateSaveName()
	{
		String sv = "UWNHRando." + inputLines[0].txt.getText() + "." + inputLines[2].txt.getText() + ".smc";
		inputLines[3].setTextText(sv);
	}
	
	private void saveConfig()
	{
		String c1 = "[Source ROM]=" + inputLines[1].txt.getText();
		String c2 = "[Last Flags]=" + inputLines[2].txt.getText();
		String c3 = "[Internal: saveToRomDir]=" + this.romsToOrigROMDir;
		String config = c1 + "\n" + c2 + "\n" + c3 + "\n";
		String currDir = System.getProperty("user.dir");
		String path = currDir + File.separator + "UWNHRandoConfig.txt";
		File f = new File(path);
		try
		{
			if(!f.exists())
				f.createNewFile();
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(config);
			bw.close();
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		
	}
	
	private void setupPanels()
	{
		//the Map tag
		JPanel mapPanel = new JPanel(new GridLayout(2,2));
		opts = new ArrayList<FlagPanel>();
		
		String[] mapT = {"Randomize Map", "View minimap of generated ROM", "Randomize Starting Ship", 
				         "Randomize Initial Price Indeces", "Randomize goods sold at all market types",
				         "Eliminate storms in the known world", "Eliminate new ship building time",
				         "24/7 shop and services availability"};
		String[] mapF = {"M", "V", "S", "R", "K", "s", "b", "t"};
		FlagPanel fp = new FlagPanel("General", mapT, mapF, true, false, false, this);
		mapPanel.add(fp);
		opts.add(fp);
		
		String[] contT = {"# of Continents"};
		String[] contF = {"C"};
		fp = new FlagPanel("Continents", contT, contF, false, false, true, this);
		fp.setMinVal(1);
		fp.setMaxVal(6);
		mapPanel.add(fp);
		opts.add(fp);
		
		String[] islT = {"# of Islands"};
		String[] islF = {"I"};
		fp = new FlagPanel("Islands", islT, islF, false, false, true, this);
		fp.setMinVal(5);
		fp.setMaxVal(15);
		mapPanel.add(fp);
		opts.add(fp);
		
		String[] polT = {"Polar Ice Thickness"};
		String[] polF = {"O"};
		fp = new FlagPanel("Polar Ice", polT, polF, false, false, true, this);
		fp.setMinVal(2);
		fp.setMaxVal(8);
		mapPanel.add(fp);
		opts.add(fp);
		//theme(table)  list(cols)   items(rows)
		//the Theme tag
		JTabbedPane thm = new JTabbedPane();
		thm.setTabPlacement(JTabbedPane.LEFT);
		String[] themeTabs = {"Kingdoms", "Areas", "Ports", "Supply Ports", "Discoveries", "Commodities"};
		this.themeTables = themeTabs;
		String[] editable = {"Name", "Name", "Name (16 chars max);Market Type;Shipyard Type;Specialty Good;Culture", "Name (16 chars max)", "Name (16 chars max)", "Name (8 + 6 chars max)"};
		String[] mTypes = {"0.Random", "1.Iberia", "2.North Europe", "3.South Europe", "4.North Africa", "5.Ottoman Empire",
						   "6.West Africa", "7.Caribbean", "8.South America", "9.East Africa", "10.Middle East",
						   "11.India", "12.Spice Islands", "13.Far East"};
		String[] syTypes = {"0.Random","1.Balsa,Galleon","2.Barge","3.Full-Rigged","4.La Reale","5.Buss","6.Venetian Galeass",
							"7.Xebec", "8.Carrack", "9.Junk", "10.Tekkousen", "11.Pinnace,Galleon"};
		
		String[] goods = new String[origTheme[5][0].length + 2];
		goods[0] = "-1.None";
		goods[1] = "0.Random";
		for(int i = 2; i < goods.length; i++)
			goods[i] = (i - 1) + "." + origTheme[5][0][i - 2];
		
		String[] cultures = {"0.Random","1.European Shingle", "2.European Wood", "3.Caribbean Hut", "4.African Hut", "5.Middle Eastern",
							 "6.Spice Island Hut", "7.Indian", "8.Chinese", "9.Japanese"};
		
		for(int i = 0; i < themeTabs.length; i++)
		{
			String[] list = origTheme[i][0];
			String[] editCols = editable[i].split(";");
			String[][] editOpts = {{},{}};
			if(editCols.length > 1)
			{
				list = new String[origTheme[i][0].length + 1];
				list[0] = "All Ports";
				for(int j = 1; j < list.length; j++)
					list[j] = origTheme[i][0][j - 1];
				String[][] tt = {{}, {}, mTypes, syTypes, goods, cultures};
				editOpts = tt;
				String[][] uu = {{}, mTypes, syTypes, goods, cultures};
				this.portTables = uu;
			}
			ListPanel lp = new ListPanel(themeTabs[i], i, list, editCols, editOpts, this);
			thm.addTab(themeTabs[i], lp);
		}
		
		JPanel faqPan = new JPanel(new BorderLayout());
		String faq = loadTextFile("UWNHRandoFAQ.txt");
		if(faq != null)
		{
			JTextArea faqT = new JTextArea(faq);
			faqT.setColumns(80);
			faqT.setRows(25);
			faqT.setLineWrap(true);
			faqT.setWrapStyleWord(true);
			JScrollPane faqSP = new JScrollPane(faqT);
			faqPan.add(faqSP);
		}
		else
		{
			JLabel errT = new JLabel("Error: UWNHRandoFAQ.txt was not found in the same directory as the Randomizer");
			faqPan.add(errT);
		}
		
		jtp.addTab("Map", mapPanel);
		jtp.addTab("Theme", thm);
		jtp.addTab("FAQ", faqPan);
	}
	
	InputLinePanel[] setupInputLines()
	{
		
		String[] defaultInputs = {String.valueOf((long) (UWNHRando.rand() * Long.MAX_VALUE)), "", "", "", ""};
		String[] labels = {"Seed", "Rom Location", "Flags", "Output Rom", "Theme"};
		//String[] input = {"","","","",""};
		String[] buttons = {"Roll Seed", "Find...", "Clear", "Make ROM", "Save Theme"};
		InputLinePanel[] rv = new InputLinePanel[5];
		for(int i = 0; i < 5; i++)
		{
			boolean affectsSaveName = false;
			if( i ==0 )
				affectsSaveName = true;
			if(i < 4)
				rv[i] = new InputLinePanel(labels[i], defaultInputs[i], buttons[i], null, this, affectsSaveName);
			else
				rv[i] = new InputLinePanel(labels[i], defaultInputs[i], buttons[i], "Load Theme...", this, affectsSaveName);
		}
		return rv;
	}
	
	private void clearAllOptions() 
	{
		for(int i = 0; i < opts.size(); i++)
		{
			FlagPanel fp = opts.get(i);
			for(int j = 0; j < fp.checks.length; j++)
				fp.checks[j].setSelected(false);
		}
		inputLines[2].setTextText("");
		updateSaveName();
	}
	
	public void execute(InputLinePanel src, int fx)
	{
		int i = 0;
		for(; i < inputLines.length; i++)
			if(src == inputLines[i])
				break;
		
		switch(i)
		{
		case 0:
			inputLines[i].setTextText(String.valueOf((long) (UWNHRando.rand() * Long.MAX_VALUE)));
			updateSaveName();
			break;
		case 1:
			//select file
			jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int returnVal = jfc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) 
            {
                File file = jfc.getSelectedFile();
                inputLines[i].setTextText(file.getAbsolutePath());
                saveConfig();
                if(origTheme == null)
                	finishUI();
            }
			break;
		case 2:
			//inputLines[i].setTextText("");
			//clear options on current panel
			clearAllOptions();
			/*inputLines[2].setTextText("");
			updateSaveName();*/
			break;
		case 3:
			//String errorMsg = "";
			String s1 = inputLines[0].txt.getText();
			try
			{
				Long.parseLong(s1);
			}
			catch(Exception ex)
			{
				JOptionPane.showMessageDialog(this, "Seed must be a number", "Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			long seed = Long.parseLong(s1);
			UWNHRando.setSeed(seed);
			rt = new RandomizerThread();
			tim = new Timer(120000, this);
			//begin the race
			rt.start();
			tim.start();
			break;
		case 4:
			//theme
			if(fx == 0)  //save theme
			{
				saveTheme(inputLines[i].txt.getText());
			}
			else  //load theme
			{
				String currDir = System.getProperty("user.dir");
				jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				returnVal = jfc.showOpenDialog(this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) 
	            {
	                File file = jfc.getSelectedFile();
	                String ss = file.getAbsolutePath();
	                if(ss.startsWith(currDir))
	                	ss = ss.substring(currDir.length() + 1);
	                inputLines[i].setTextText(ss);
	                loadTheme(file.getAbsolutePath());
	            }
			}
			break;
		}
	}
	
	private void saveTheme(String fname)
	{
		String out = "";
		for(int t = 0; t < themeTables.length; t++)
		{
			out += "[" + themeTables[t] + "]\n";
			for(int row = 0; row < theme[t][0].length; row++)
			{
				out += theme[t][0][row];
				for(int col = 1; col < theme[t].length; col++)
				{
					String str = theme[t][col][row];
					out += ";" + str.substring(0, str.indexOf("."));
				}
				out += "\n";
			}
			out += "\n";
		}
		String currDir = System.getProperty("user.dir");
		String path = currDir + File.separator + inputLines[4].txt.getText() + ".thm";
		File f = new File(path);
		try
		{
			if(!f.exists())
				f.createNewFile();
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(out);
			bw.close();
			JOptionPane.showMessageDialog(this, path + " has been saved.", "File Saved", JOptionPane.INFORMATION_MESSAGE);
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
	}
	
	private void loadTheme(String fname)
	{
		//JOptionPane
		String ff = loadTextFile(fname);
		if(ff != null)
		{
			String[] a = ff.split("\n");
			int tab = -1;
			int row = 0;
			for(int i = 0; i < a.length; i++)
			{
				String s = a[i];
				if(s.startsWith("["))
				{
					tab++;
					row = 0;
					continue;
				}
				else if(s.length() > 0)
				{
					String[] ss = s.split(";");
					theme[tab][0][row] = ss[0];
					for(int col = 1; col < ss.length; col++)
					{
						int idx = Integer.parseInt(ss[col]);
						theme[tab][col][row] = String.valueOf(idx);//new String(this.portTables[col][idx]);
					}
					row++;
				}	
			}
			updateTheme();
		}
		else
		{
			JOptionPane.showMessageDialog(this, fname + " was not found", "File not found", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void updateTheme()
	{
		for(int tab = 0; tab < theme.length; tab++)
		{
			for(int col = 1; col < theme[tab].length; col++)
			{
				for(int row = 0; row < theme[tab][col].length; row++)
				{
					//int idx = Integer.parseInt(theme[tab][col][row]);
					for(int idx = 0; idx < portTables[col].length; idx++)
					{
						if(portTables[col][idx].startsWith(theme[tab][col][row] + "."))
						{
							theme[tab][col][row] = new String(this.portTables[col][idx]);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ev) 
	{
		// This handles the randomizerThread timing out AND the check boxes
		if(ev.getSource() == bx1)
		{
			boolean sel1 = bx1.isSelected();
			this.romsToOrigROMDir = sel1;
			bx2.setSelected(!sel1);
			saveConfig();
		}
		else if(ev.getSource() == bx2)
		{
			boolean sel2 = bx2.isSelected();
			this.romsToOrigROMDir = !sel2;
			bx1.setSelected(!sel2);
			saveConfig();
		}
		else if(rt != null)
		{
			rt.interrupt();
			rt = null;
			JOptionPane.showMessageDialog(this, "This seed timed out when generating a ROM.\nTry a different seed.", "Sorry", JOptionPane.ERROR_MESSAGE);
			tim.stop();
		}
	}
	
	
}


public class UWNHRando 
{
	private static Random rando;
	public static String outputRomName;
	
	public static double rand()
	{
		return rando.nextDouble();
	}
	
	public static void setSeed(long seed)
	{
		rando = new Random(seed);
		//randSeed = seed;
	}
	
	public static void setOutputRomName(String in)
	{
		outputRomName = in; 
	}
	
	private static ArrayList<Integer> getMapData(SNESRom rom)
	{
		int mapBaseLoc = 1163902; //11c72e; the 512 header is shaved off during decompression
		int deltaLoc = 249786 - 512;  //3cfba
		ArrayList<Integer> rv = new ArrayList<Integer>();
		for(int yy = 0; yy < 45; yy++)
		{
			for(int xx = 0; xx < 90; xx++)
			{
				int delta = 0;
				for(int i = 0; i < 3; i++)
				{
					int d = (int) (rom.data[deltaLoc + i] & 255);
					delta += (d << (8 * i));
				}
				rv.add(mapBaseLoc + delta);
				deltaLoc += 3;
			}
		}
		return rv;
	}
	
	public static String getPortName(int idx, SNESRom rom) 
	{
		int loc = 577879 - 512 + (22 * idx);
		String rv = "";
		while(true)
		{
			byte bb = rom.data[loc];
			if(bb == 0)
				return rv;
			char c = (char) rom.data[loc];
			rv += c;
			loc++;
		}
	}
	
	public static int[] getPortXY(int idx, SNESRom rom)
	{
		int loc = 577875 - 512 + (22 * idx);
		int[] rv = {0, 0};
		for(int i = 0; i < 2; i++)
		{
			int s1 = (rom.data[loc] & 255);
			int s2 = (rom.data[loc + 1] & 255);
			rv[i] = ((s2 << 8) + s1);
			if(rv[i] < 0)
				System.out.println("X or Y is < 0");
			loc += 2;
		}
		return rv;
	}
	
	private static void testRomDump(SNESRom rom)
	{
		rom.randomizeMap(3, 12, 4, true);
		if(rom.romInvalid == 0)
			rom.dumpRom("UWNewRom1.smc", true);
		else
		{
			System.out.println("Failed to generate valid ROM.");
			return;
		}
		try
		{
			SNESRom rom2 = new SNESRom(rom.homeDir + "UWNewRom1.smc");
			//Compressor comp = new Compressor(rom2);
			GameMap gm = new GameMap();
			gm.decompressOrigMap(rom2);
			GameMapWindow gmw = new GameMapWindow(gm);
			//viewPort(rom2);
		}
		catch(Exception ex)
		{
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}
		
	}
	
	public static void getPortFindData(SNESRom rom)
	{
		Port[] allPorts = collectPorts(rom, false);
		int[] counts = new int[256];
		ArrayList<String>[] locs = new ArrayList[256];
		for(int i = 0 ;i < 256; i++)
			locs[i] = new ArrayList<String>();
		int conflicts = 0;
		for(int i = 0; i < allPorts.length; i++)
		{
			Port p = allPorts[i];
			short vv = (short) ((p.x + p.y) % 255);
			String ss1 = p.name + "  at " + p.x + " " + p.y + "  sumMod255=";
			String str = Integer.toHexString(i);
			if(str.length() == 1)
				str = "0" + str;
			//if((i & 15) == 0)
				//System.out.println();
			System.out.println(str + "." + ss1 + vv);
			counts[vv]++;
			locs[vv].add(str + ":" + p.name);
			if(counts[vv] >= 2)
				conflicts++;
		}
		System.out.println(conflicts + " conflicts");
		for(int i = 0; i < 256; i++)
			if(counts[i] > 1)
				System.out.println("Conflict at " + i + " counts[i]=" + counts[i] + "   " + locs[i].toString());
	}

	private static void viewPort(SNESRom rom)
	{
		/*ArrayList<Integer> portData = getPortData(rom);
		int pStart = portData.get(pindex);
		//start = all.get(k);
		Compressor comp = new Compressor(rom);
		ArrayList<Byte> ba = comp.gameDecompress(pStart); 
		byte tileset = rom.data[735038 - 512 + pindex];
		Port p = new Port(pindex);
		p.tileType = tileset;
		p.load(ba);*/
		PortWindow pw = new PortWindow(rom);
	}
	
	public static Port[] collectPorts(SNESRom rom, boolean calculatePortDiffs)
	{
		ArrayList<Integer> portData = getPortData(rom);
		int[][] portSizeDiffs = new int[portData.size()][9];
		Port[] allPorts = new Port[portData.size()];
		Compressor comp = new Compressor(rom);
		for(int pindex = 0; pindex < portData.size(); pindex++)
		{
			int pStart = portData.get(pindex);
			ArrayList<Byte> orig = new ArrayList<Byte>(500);
			int start = pStart;
			int end = 0;
			if(pindex < portData.size() - 1)
				end = portData.get(pindex + 1);
			else
				end = 1738904 + 29770;
			for(int i = start; i < end; i++)
				orig.add(rom.data[i - 512]);
			//start = all.get(k);
			ArrayList<Byte> ba = comp.gameDecompress(pStart); 
			int origSize = comp.dataSize;
			byte tileset;
			if(rom.romOrigTheme == null)  //then a theme has not been applied to the rom
				tileset = rom.data[735038 - 512 + pindex];
			else
				tileset = (byte) (Byte.parseByte(rom.romOrigTheme[2][4][pindex]) - 1);
			Port p = new Port(pindex);
			p.tileType = tileset;
			p.origTileType = tileset;
			p.compressedBytes = orig;
			p.load(ba);
			p.setName(UWNHRando.getPortName(pindex, rom));
			int[] pxy = UWNHRando.getPortXY(pindex, rom);
			p.setXY(pxy[0], pxy[1]);
			p.backup();
			if(calculatePortDiffs)
			{
				for(int i = 0; i < 9; i++)
				{
					p.restoreBackup();
					boolean converted = p.convertTileType(i, null);
					ArrayList<Byte> p2 = p.getConvertedPort();
					byte[] pa = new byte[p2.size()];
					for(int j = 0; j < p2.size(); j++)
						pa[j] = p2.get(j);
					ArrayList<Byte> newPort = comp.compressBytes(pa);
					int diff = newPort.size() - origSize;
					if(!converted)
					{
						if(i == p.tileType)
						{
							if(diff > 0)
								diff = 0;
						}
						else
							diff = 10000;
					}
					portSizeDiffs[pindex][i] = diff;
				}
				p.restoreBackup();
			}
			allPorts[pindex] = p;
		}
		if(calculatePortDiffs)
			Port.sizeDiffs = portSizeDiffs;
		return allPorts;
	}
	
    static Port[] selectPortStyles(GameMap.PortData[] pdata, SNESRom rom)  //preset can be a list of allowed bitfield
	{
		Port[] allPorts = collectPorts(rom, true);
		ArrayList<Integer> portData = getPortData(rom);
		
		int sz = portData.size();
		//int[] rv = new int[sz];
		int total = 0;
		boolean random = false;
		ArrayList<Integer> allowedTypes = new ArrayList<Integer>();
		
		for(int i = 0; i < sz; i++)
		{
			random = true;
			int pt = -1;
			if(rom.data[735038 - 512 + i] >= 0)  //only randomize those ports with tiletype -1 (random)
			{
				pdata[i].origCulture = allPorts[i].origTileType;
				pdata[i].finalCulture = rom.data[735038 - 512 + i];
				pt = pdata[i].finalCulture;
				random = false;
			}
			else if(pdata[i].isCapital)  //capitals require the palace tileset available only in styles 0, 1 and 4
			{
				//if((pdata[i].possCultures & 1) > 0)
				allowedTypes.add(0);
				allowedTypes.add(1);
				allowedTypes.add(4);
			}
			else
			{
				for(int j = 0; j < 9; j++)
				{
					if(((1 << j) & pdata[i].possCultures) > 0)
						allowedTypes.add(j);
				}
			}
			if(random)
			{
				int r = (int) (UWNHRando.rand() * allowedTypes.size());
				pt = allowedTypes.get(r);
				if(Port.sizeDiffs[i][pt] == 10000)
				{
					allowedTypes.clear();
					allowedTypes.add(0);
					allowedTypes.add(1);
					allowedTypes.add(4);
					i--;
					continue;
				}
			}
			total += Port.sizeDiffs[i][pt];
			//allPorts[i].tileType = (byte) pt;  //no!
			pdata[i].origCulture = allPorts[i].tileType;
			pdata[i].finalCulture = pt;
			allowedTypes.clear();
		}
		if(total > 0)
			return null;
		else
			return allPorts;
	}
	
	private static void testAllPorts(SNESRom rom)
	{
		ArrayList<Integer> portData = getPortData(rom);
		int[] skipList = {};
		int totalErrors = 0;
		for(int pindex = 0; pindex < portData.size(); pindex++)
		{
			if(Arrays.binarySearch(skipList, pindex) >= 0)
				continue;
			int pStart = portData.get(pindex);
			//start = all.get(k);
			Compressor comp = new Compressor(rom);
			ArrayList<Byte> ba = comp.gameDecompress(pStart); 
			int origSize = comp.dataSize;
			byte tileset = rom.data[735038 - 512 + pindex];
			Port p = new Port(pindex);
			p.tileType = tileset;
			p.origTileType = tileset;
			p.load(ba);
			p.setName(UWNHRando.getPortName(pindex, rom));
			p.backup();
			String out = p.name;
			int a = out.length();
			for(int i = a; i < 16; i++)
				out += " ";
			out += origSize + "  ";
			for(int i = 0; i < 9; i++)
			{
				p.restoreBackup();
				boolean converted = p.convertTileType(i, null);
				//if(converted)
				//{
					//p.testPort(i);
					//totalErrors += p.buildingsFailed;
					ArrayList<Byte> p2 = p.getConvertedPort();
					byte[] pa = new byte[p2.size()];
					for(int j = 0; j < p2.size(); j++)
						pa[j] = p2.get(j);
					
					
				//}
				ArrayList<Byte> newPort = comp.compressBytes(pa);
				byte[] testp = new byte[newPort.size()];
				for(int j = 0; j < testp.length; j++)
					testp[j] = newPort.get(j);
				ArrayList<Byte> test2 = comp.decompress(0, testp);
				if(test2.size() != p2.size())
					System.out.println("Size difference in port output expected=" + p2.size() + " acutal=" + test2.size());
				for(int j = 0; j < p2.size(); j++)
				{
					if(test2.get(j) != p2.get(j))
						System.out.println("Error in output at index " + j + " expected=" + p2.get(j) + " acutal=" + test2.get(j));
				}
				int diff = newPort.size() - origSize;
				String dfstr = String.valueOf(diff);
				
				while(dfstr.length() < 3)
					dfstr = " " + dfstr;
				out += i +":" + newPort.size() + " diff=" + dfstr + ";  ";
			}
			System.out.println(out);
		}
		//System.out.println("All ports tested; " + totalErrors + " errors found.");
	}
	
	public static ArrayList<Integer> getPortData(SNESRom rom)  //returns port decompresssion deltas
	{
		int start = 1738904;  //1a8898
		int deltas = 650959 - 512; //9eecf
		ArrayList<Integer> rv = new ArrayList<Integer>();
		for(int k = 0; k < 100; k++)
		{
			int delta = 0;
			for(int i = 0; i < 3; i++)
			{
				int d = (int) (rom.data[deltas + i] & 255);
				delta += (d << (8 * i));
			}
			rv.add(start + delta);
			deltas += 3;
		}
		return rv;
	}
	
	public static int countFutureBytes(ArrayList<Byte> controlList)
	{
		int rv = 0;
		for(int i = 0; i < controlList.size(); i++)
			if(controlList.get(i) == 3)
				rv++;
		return rv;
	}
	
	/*private static void compareCompressedData(byte[] ds1, byte[] ds2)
	{
		ArrayList<Byte> d1 = new ArrayList<Byte>();
		ArrayList<Byte> d2 = new ArrayList<Byte>();
		
		
	}*/
	
	private static boolean testMapgen(SNESRom rom)
	{
		//rom.listLandTilesets();
		GameMap gm = new GameMap();
		//gm.decompressOrigMap(rom);
		
		int nContinents = 4;
		int nIslands = 10;
		gm.makeMap(nContinents, nIslands, 3);
		//gm.smoothWater(8);
		gm.makeIceCaps(3);
		gm.mountainAndForestFix();
		//GameMapWindow gmw = new GameMapWindow(gm);
		Compressor cc = new Compressor(rom);
		ArrayList<ArrayList<Byte>> bts = cc.compressMap(gm);
		
		int sz = cc.getMapSize(bts);
		int maxLen = 8;
		int refreshCount = 0;
		while(sz > 80000)
		{
			gm.restoreBackup();
			int sunk = 0;
			while(sunk < 5000)
			{
				int sunk2 = gm.smoothWater(maxLen);
				sunk += sunk2;
				if(sunk2 < 100 || refreshCount == 10)
				{
					maxLen--;
					refreshCount = 0;
				}
				//if(maxLen < 4)
					//break;
			}
			//gm.simplifyBlocks(10, bts, rom);
			gm.makeIceCaps(3);
			gm.mountainAndForestFix();
			bts = cc.compressMap(gm);
			refreshCount++;
			sz = cc.getMapSize(bts);
		}
		if(sz < 25000)
			return false;
		//GameMapWindow gmw = new GameMapWindow(gm);
		//we have a viable map
		//now let's test it
		int[] avail = {90000};
		int[] availLoc = {0};
		cc.collateMap(bts, avail, gm);  //for the test, the map will not be split up
		int[][] deltas = cc.getDeltas();
		int[][] deltaI = cc.getDeltaIndeces();
		byte[][] dmap = new byte[1080][2160];
		
		byte[] cm = gm.getCompressedMap();
		
		for(int yy = 0; yy < 45; yy++)
		{
			for(int xx = 0; xx < 90; xx++)
			{
				//System.out.println("block [" + xx + "," + yy + "] is using data at " + deltas[yy][xx]);
				ArrayList<Byte> blk = cc.decompress(deltas[yy][xx], cm);
				ArrayList<Byte> orig = bts.get(deltaI[yy][xx] * 2);
				
				int ii = 0;
				int errVal = 0;
				int errValO = 0;
				int blkNum = yy * 90 + xx; 
				for(int y = 0; y < 24; y += 2)
				{
					for(int x = 0; x < 24; x += 2)
					{
						int val = (int) (blk.get(ii) & 255);
						int oval = (int) (orig.get(ii) & 255);
						if(val != oval)
							System.out.println("Mismatch in original data; orig=" + oval + "  new=" + val);
						ii++;
						int yo = yy * 24 + y;
						int xo = xx * 24 + x;
						errVal = val;
						errValO = oval;
						if(val < 16)
						{
							if((val & 1) > 0)
								dmap[yo][xo] = -128;
							else
								dmap[yo][xo] = 0;
							if((val & 2) > 0)
								dmap[yo + 1][xo] = -128;
							else
								dmap[yo + 1][xo] = 0;
							xo++;
							if((val & 4) > 0)
								dmap[yo][xo] = -128;
							else
								dmap[yo][xo] = 0;
							if((val & 8) > 0)
								dmap[yo + 1][xo] = -128;
							else
								dmap[yo + 1][xo] = 0;
							xo--;
						}
						else
						{
							val <<= 2;
							/*if(val > hiVal)
								hiVal = val;*/
							int tile = 983552 + val - 512; //f0200
							int[] look = new int[4];//rom.data[tile];
							for(int i = 0; i < 4; i++)
								look[i] = (rom.data[tile + i] & 255);
							dmap[yo][xo] = rom.data[tile];
							dmap[yo + 1][xo] = rom.data[tile + 1];
							dmap[yo][xo + 1] = rom.data[tile + 2];
							dmap[yo + 1][xo + 1] = rom.data[tile + 3];
							//for(int i = 0; i < 4; i++)
								//tileCounts[(int) (rom.data[tile + i] & 255)]++;
						}
						byte mismatch = 0;
						for(int ty = 0; ty < 2; ty++)
							for(int tx = 0; tx < 2; tx++)
								if(dmap[yo + ty][xo + tx] != gm.fullMap[yo + ty][xo + tx])
									mismatch |= 1 << (ty * 2 + tx);
						if(mismatch > 0)
						{
							System.out.println("Block mismatch; mismatch=" + mismatch);
							for(int ty = 0; ty < 2; ty++)
								for(int tx = 0; tx < 2; tx++)
									System.out.println("Block #" + blkNum + ": Difference in map at " + (xo + tx) + "," + (yo + ty) + "; orig=" + gm.fullMap[yo+ty][xo+tx] + "  new=" + dmap[yo+ty][xo+tx] + "  errValO=" + errValO + " errVal=" + errVal);
						}
					}
				}
				
				/*for(int y = 0; y < 24; y++)
				{
					for(int x = 0; x < 24; x++)
					{
						int yo = yy * 24 + y;
						int xo = xx * 24 + x;
						
					}
				}*/
			}
		}
		GameMap gm2 = new GameMap(dmap);
		GameMapWindow gmw2 = new GameMapWindow(gm2);
		
		boolean s = gm.compareTo(gm2);
		if(!s)
			System.out.println("Map not successfully compressed");
		gm2.adjOrigTilemap();
		return true;
	}
	
	private static void testCompress2(SNESRom rom)   //tests new compression algorithm
	{
		Compressor comp = new Compressor(rom);
		//GameMap gm = new GameMap();
		//gm.decompressOrigMap(rom);
		int errors = 0;
		int sumSize1 = 0;
		int sumSize2 = 0;
		
		int start = 1163902;  //map - test complete
		ArrayList<Integer> all = getMapData(rom);
		
		int startLoc[] = new int[5000];
		for(int i = 0; i < all.size(); i++)
		{
			int ss = all.get(i);
			boolean found = false;
			for(int j = i; j >= 0; j--)
			{
				if(startLoc[j] == ss)
				{
					found = true;
					break;
				}
			}
			if(!found)
				startLoc[i] = ss;
		}
		
		for(int k = 0; k < all.size(); k++)
		{
			if(startLoc[k] == 0)
				continue;
			//decompress it => decompressed A [AListByte]   //map zones start at 11c27e; ports start at 1a8898
			start = all.get(k);
			ArrayList<Byte> ba = comp.gameDecompress(start); 
			int ds1 = comp.dataSize;
			sumSize1 += ds1;
			byte[] barr = new byte[ba.size()];
			for(int i = 0; i < barr.length; i++)
				barr[i] = ba.get(i);
			ArrayList<Byte> rc = comp.compressBytes2(barr);
			byte[] rcarr = new byte[rc.size()];
			int ds2 = rcarr.length;
			for(int i = 0; i < rcarr.length; i++)
				rcarr[i] = rc.get(i);
			ArrayList<Byte> bb = comp.decompress2(0, rcarr);
			sumSize2 += comp.dataSize;
			if(ba.size() != bb.size())
			{
				System.out.println("Difference in decompressed size:" + ba.size() + "   " + bb.size());
				errors += Math.abs(ba.size() - bb.size());
			}
			for(int i = 0; i < Math.min(ba.size(), bb.size()); i++)
			{
				if(ba.get(i) != bb.get(i))
				{
					System.out.println("Difference in decompressed data at " + i + " expected " + ba.get(i) + " actual " + bb.get(i));
					errors++;
				}
			}
			System.out.println("--- DONE TESTING BLOCK #" + k + "; " + errors + " cumulative errors; " + sumSize1 + " cumulative original size; " + sumSize2 + " cumulative recompressed size ---");
		}
		
	}
	
	private static void testControlCompression(SNESRom rom)
	{
		Compressor comp = new Compressor(rom);
		
		int start = 1163902;  //map - test complete
		ArrayList<Integer> all = getMapData(rom);
		
		byte shift = 7;
		ArrayList<Byte> control2 = new ArrayList<Byte>();
		byte val2 = 0;
		int lastC2 = 0;
		ArrayList<Byte> control3 = new ArrayList<Byte>();
		byte val3 = 0;
		int lastC3 = 0;
		ArrayList<Byte> control4 = new ArrayList<Byte>();
		byte val4 = 0;
		int lastC4 = 0;
		/*ArrayList<Byte> controlL = new ArrayList<Byte>();
		byte valL = 0;
		int lastCL = 0;*/
		
		int sum1 = 0;
		int sum2 = 0;
		int sum3 = 0;
		int sum4 = 0;
		
		int startLoc[] = new int[5000];
		for(int i = 0; i < all.size(); i++)
		{
			int ss = all.get(i);
			boolean found = false;
			for(int j = i; j >= 0; j--)
			{
				if(startLoc[j] == ss)
				{
					found = true;
					break;
				}
			}
			if(!found)
				startLoc[i] = ss;
		}
		
		for(int k = 5; k < all.size(); k++)
		{
			if(startLoc[k] == 0)
				continue;
			
			start = all.get(k);
			ArrayList<Byte> ba = comp.gameDecompress(start); 
			ArrayList<Byte> cm = comp.controlMarking;
			ArrayList<Byte> allControl = new ArrayList<Byte>();
			for(int i = 0; i < cm.size(); i++)
			{
				int loc = start - 512 + i;
				if(cm.get(i) == 2)
				{
					allControl.add(rom.data[loc]);
					sum1++;
				}
			}
			byte on = 1;
			val2 = 0;
			val3 = 0;
			val4 = 0;
			for(int i = 0; i < allControl.size(); i++)
			{
				shift = 7;
				while(shift >= 0)
				{
					byte c = allControl.get(i);
					byte val = (byte) (c & (1 << shift));
					if(val != 0)
						val = 1;
					if(val == on)
					{
						val2++;
						if(val2 == 3)
						{
							control2.add(val2);
							val2 = 0;
						}
						val3++;
						if(val3 == 7)
						{
							control3.add(val3);
							val3 = 0;
						}
						val4++;
						if(val4 == 15)
						{
							control4.add(val4);
							val4 = 0;
						}
					}
					else
					{
						//write each value
						control2.add(val2);
						control3.add(val3);
						control4.add(val4);
						val2 = 1;
						val3 = 1;
						val4 = 1;
						//toggle
						on ^= 1;
					}
					shift--;
				}
			}
			control2.add(val2);
			control3.add(val3);
			control4.add(val4);
			
			ArrayList<Byte> control2F = new ArrayList<Byte>();
			val2 = 0;
			shift = 0;
			for(int i = 0; i < control2.size(); i++)
			{
				val2 <<= 2;
				val2 |= control2.get(i);
				shift += 2;
				if(shift == 8)
				{
					control2F.add(val2);
					shift = 0;
					val2 = 0;
				}
			}
			if(shift > 0)
				control2F.add(val2);
			
			ArrayList<Byte> control3F = new ArrayList<Byte>();
			val3 = 0;
			shift = 0;
			for(int i = 0; i < control3.size(); i++)
			{
				//val3 <<= 3;
				if(shift == 6)
				{
					val3 <<= 2;
					val3 |= ((control3.get(i) & 6) >> 1);
					control3F.add(val3);
					val3 = (byte) (control3.get(i) & 1);
					shift = 1;
				}
				else if(shift == 7)
				{
					val3 <<= 1;
					val3 |= ((control3.get(i) & 4) >> 2);
					control3F.add(val3);
					val3 = (byte) (control3.get(i) & 3);
					shift = 2;
				}
				else
				{
					val3 <<= 3;
					val3 |= control3.get(i);
					shift += 3;
				}
				if(shift == 8)
				{
					control3F.add(val3);
					shift = 0;
					val3 = 0;
				}
			}
			if(shift > 0)
				control3F.add(val3);
			
			ArrayList<Byte> control4F = new ArrayList<Byte>();
			val2 = 0;
			shift = 0;
			for(int i = 0; i < control4.size(); i++)
			{
				val2 <<= 4;
				val2 |= control4.get(i);
				shift += 4;
				if(shift == 8)
				{
					control4F.add(val2);
					shift = 0;
					val2 = 0;
				}
			}
			if(shift > 0)
				control4F.add(val2);
			
			sum2 += control2F.size();
			sum3 += control3F.size();
			sum4 += control4F.size();
			control2.clear();
			control3.clear();
			control4.clear();
			control2F.clear();
			control3F.clear();
			control4F.clear();
		}
		
		System.out.println("Control bytes size summary:");
		System.out.println("Orig=" + sum1);
		System.out.println("2Bit=" + sum2);
		System.out.println("3Bit=" + sum3);
		System.out.println("4Bit=" + sum4);
		
	}
	
	private static void testCompress3(SNESRom rom)
	{
		Compressor comp = new Compressor(rom);
		//GameMap gm = new GameMap();
		//gm.decompressOrigMap(rom);
		int errors = 0;
		int sumSize1 = 0;
		int sumSize2 = 0;
		
		int start = 1163902;  //map - test complete
		ArrayList<Integer> all = getMapData(rom);
		
		int startLoc[] = new int[5000];
		for(int i = 0; i < all.size(); i++)
		{
			int ss = all.get(i);
			boolean found = false;
			for(int j = i; j >= 0; j--)
			{
				if(startLoc[j] == ss)
				{
					found = true;
					break;
				}
			}
			if(!found)
				startLoc[i] = ss;
		}
		
		for(int k = 0; k < all.size(); k++)
		{
			if(startLoc[k] == 0)
				continue;
			//decompress it => decompressed A [AListByte]   //map zones start at 11c27e; ports start at 1a8898
			start = all.get(k);
			ArrayList<Byte> ba = comp.gameDecompress(start); 
			int ds1 = comp.dataSize;
			sumSize1 += ds1;
			byte[] barr = new byte[ba.size()];
			for(int i = 0; i < barr.length; i++)
				barr[i] = ba.get(i);
			ArrayList<Byte> rc = comp.compressBytes3(barr, 12, false);
			byte[] rcarr = new byte[rc.size()];
			int ds2 = rcarr.length;
			for(int i = 0; i < rcarr.length; i++)
				rcarr[i] = rc.get(i);
			ArrayList<Byte> bb = comp.decompress3(0, rcarr, 12, 144, barr, false);
			sumSize2 += comp.dataSize;
			if(ba.size() != bb.size())
			{
				System.out.println("Difference in decompressed size:" + ba.size() + "   " + bb.size());
				errors += Math.abs(ba.size() - bb.size());
			}
			for(int i = 0; i < Math.min(ba.size(), bb.size()); i++)
			{
				if(ba.get(i) != bb.get(i))
				{
					System.out.println("Difference in decompressed data at " + i + " expected " + ba.get(i) + " actual " + bb.get(i));
					errors++;
				}
			}
			System.out.println("--- DONE TESTING BLOCK #" + k + "; " + errors + " cumulative errors; " + sumSize1 + " cumulative original size; " + sumSize2 + " cumulative recompressed size ---");
		}
	}
	
	private static void testCompress(SNESRom rom)
	{
		Compressor comp = new Compressor(rom);
		//get some map compressed data   -note dataSize
		
		
		
		System.out.println("");
		
		
		int errors = 0;
		int sumSize1 = 0;
		int sumSize2 = 0;
		
		//int start = 1163902;  //map - test complete
		//ArrayList<Integer> all = getMapData(rom);
		
		int start = 1738904;   //ports - test complete
		ArrayList<Integer> all = getPortData(rom);
		
		
		for(int k = 0; k < all.size(); k++)
		{
			//if(startLoc[k] == 0)
				//continue;
			//decompress it => decompressed A [AListByte]   //map zones start at 11c27e; ports start at 1a8898
			start = all.get(k);
			ArrayList<Byte> ba = comp.gameDecompress(start); 
			
			
			//recompress it, glance size     -note dataSize
			int ds1 = comp.dataSize;
			/*System.out.print("Compressed bytes:");
			for(int i = 0; i < ds1; i++)
				System.out.print(viewByte(rom.data[start - 512 + i], comp.controlMarking, i) + ",");
			System.out.println();*/
			//if(k == 3)
				//comp.gameDecompress(start);
			byte[] barr = new byte[ba.size()];
			System.out.print("Decompressed bytes:");
			for(int i = 0; i < barr.length; i++)
			{
				barr[i] = ba.get(i);
				System.out.print(barr[i] + ",");
			}
			System.out.println();
			ArrayList<Byte> rc = comp.compressBytes(barr);
			
			byte[] rcarr = new byte[rc.size()];
			/*System.out.print("Compressed bytes:");
			for(int i = 0; i < rcarr.length; i++)
			{
				rcarr[i] = rc.get(i);
				System.out.print(viewByte(rcarr[i]) + ",");
			}
			System.out.println();*/
			int ds2 = rcarr.length;
			//System.out.println("Done recompressing data; old size=" + ds1 + "   new size=" + ds2);
			
			System.out.println("Compresssion pairs:");
			comp.viewCompressionPairs();
			
			//compare compressed data
			System.out.println("Comparing compressions:");
			System.out.print("orig:");
			//int cStream = 0;
			for(int i = 0; i < ds1; i++)
				System.out.print(viewByte(rom.data[start - 512 + i], comp.controlMarking, i) + ",");
			System.out.println();
			System.out.print(" new:");
			for(int i = 0; i < rcarr.length; i++)
			{
				rcarr[i] = rc.get(i);
				System.out.print(viewByte(rcarr[i], comp.compMarking, i) + ",");
			}
			System.out.println();
			sumSize1 += ds1;
			sumSize2 += (rcarr.length - countFutureBytes(comp.compMarking));
			//compare the decompression
			//decompress again => decompressed B  [AListByte]
			int ierrors = errors;
			ArrayList<Byte> bb = comp.decompress(0, rcarr);
			if(ba.size() != bb.size())
			{
				System.out.println("Difference in decompressed size:" + ba.size() + "   " + bb.size());
				errors += Math.abs(ba.size() - bb.size());
			}
			for(int i = 0; i < Math.min(ba.size(), bb.size()); i++)
			{
				if(ba.get(i) != bb.get(i))
				{
					System.out.println("Difference in decompressed data at " + i + " expected " + ba.get(i) + " actual " + bb.get(i));
					errors++;
				}
			}
			if(errors > ierrors)
				comp.compressBytes(barr);
			System.out.println("--- DONE TESTING BLOCK #" + k + "; " + errors + " cumulative errors; " + sumSize1 + " cumulative original size; " + sumSize2 + " cumulative recompressed size ---");
		}
	}
	
	public static String viewByte(byte val, ArrayList<Byte> control, int index)
	{
		String rv = viewByte(val);
		Byte b = control.get(index);
		String lst = "sdcf";
		rv += lst.charAt(b);
		if(b == 0)
		{
			int sindex = 0;
			for(int i = 0; i < index; i++)
				if(control.get(i) == 0)
					sindex++;
			sindex ^= 1;
			rv += sindex;
		}
		return rv;
	}

	public static String viewByte(byte val)
	{
		String rv = Long.toBinaryString(val);
		if(rv.length() > 8)
			rv = rv.substring(rv.length() - 8);
		while(rv.length() < 8)
			rv = "0" + rv;
		return rv;
	}
	
	
	public static void main(String[] args)
	{
		setSeed((long) (Math.random() * Long.MAX_VALUE));
		RandomizerWindow rw = new RandomizerWindow();
	}
	
	public static void mainStormView()
	{
		try 
		{
			SNESRom uwnh = new SNESRom("C:\\Users\\aearm\\Desktop\\x86-64\\Uncharted Waters - New Horizons.smc");
			//Compressor comp = new Compressor(uwnh);
			GameMap map = new GameMap();
			map.decompressOrigMap(uwnh);
			ArrayList<Integer> ev = uwnh.getEventRects();
			GameMapWindow gmw = new GameMapWindow(map, ev);
			
			BufferedImage mapImage = gmw.getMapImage();
			File outFile = new File("C:\\Users\\aearm\\Desktop\\UWNHRando\\GameMap.png");
			if(!outFile.exists())
				outFile.createNewFile();
			ImageIO.write(mapImage, "png", outFile);
			//outFile.close();
			
			uwnh.stormReport();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void mainTest(String[] args) 
	{
		// TODO Auto-generated method stub
		try 
		{
			SNESRom uwnh = new SNESRom("C:\\Users\\aearm\\Desktop\\x86-64\\Uncharted Waters - New Horizons.smc");
			int ss = 941166;
			int ee = 966753;
			byte[] cmp = uwnh.getCompressedText(ss, ee, true);
			byte[] decmp = uwnh.getDecompressedText(cmp);
			byte[] bts = uwnh.getBytes(ss, ee, true);
			for(int i = 0; i < bts.length; i++)
			{
				if(bts[i] != decmp[i])
					System.out.println("Error at " + i + ": output was " + decmp[i] + " expected " + bts[i]);
			}
			byte[] aa = uwnh.strToBytes("ff 00 f0 0f");
			String bb = uwnh.bytesToStr(aa);
			System.out.println("StrToBytes test: " + bb);
			
			byte[] test = uwnh.getBytes(1739714, 1739730, true);
			String testStr = uwnh.bytesToStr(test);
			System.out.println("Data at decompress:" + testStr);
			byte t1 = -1;
			byte t2 = 0;
			short t3 = (short) ((t2 << 8) | (t1 & 255));
			System.out.println(t3);
			Compressor comp = new Compressor(uwnh);
			//ArrayList<Byte> out = comp.gameDecompress(1739714);
			
			/*int sz = out.size();
			int usedBytes = comp.dataSize;
			System.out.println("Compressed data size:" + usedBytes + "    Decompressed data size:" + sz);
			double ratioc = (usedBytes * 1.0) / (sz * 1.0);
			System.out.println("ratio=" + ratioc);
			byte[] dcb = new byte[out.size()];
			for(int i = 0; i < dcb.length; i++)
				dcb[i] = out.get(i);*/
			
			/*String ls = "";
			for(int i = 0; i < 50; i++)
				ls += uwnh.byteToStr(dcb[i]) + " ";
			System.out.println("First 50:" + ls);*/
			/*System.out.println("Let's view last 8");
			String[] s = new String[3];
			s[0] = "";
			s[1] = "";
			s[2] = "";
			String s5 = "";
			for(int i = 8; i >= 0; i--)
			{
				s5 += viewByte(cmp[cmp.length - i - 1]);
				s[1] += viewByte(decmp[bts.length - i - 1]) + " ";
				s[2] += viewByte(bts[bts.length - i - 1]) + " ";
			}
			int xx = 0;
			int j = -1;
			while(xx != 16)
			{
				
				j++;
				String sx = s5.substring(j, j + 5);
				xx = Integer.parseInt(sx, 2);
			}
			xx = 0;
			for(int i = j; i < s5.length(); i++)
			{
				s[0] += s5.charAt(i);
				xx++;
				if(xx % 5 == 0)
					s[0] += ' ';
			}
			
			for(int i = bts.length; i < decmp.length; i++)
				s[1] += viewByte(decmp[i]) + " ";
			
			for(int i = 0; i < 3; i++)
				System.out.println(s[i]);*/
			int olds = (ee - ss);
			System.out.println("Old size " + olds + " bytes");
			System.out.println("New size " + cmp.length + " bytes");
			double ratio = (cmp.length * 1.0 / olds * 1.0);
			System.out.println("Ratio: " + ratio);
			System.out.println("Saved: " + (olds - cmp.length) + " bytes");
			System.out.println("======================================");
			String map1 = //"34 0F 0F 0C 01 03 00 00 00 00 00 00 0F 36 30 0B " +
                          "02 0C 0C 0D 0A 00 00 04 00 0C 0D 34 36 0F 0F 02 " +
                          "0A 00 00 05 02 00 03 07 0E 00 10 0A 0C 00 00 00 " +
                          "36 0F 36 0A 00 00 04 0B 00 01 00 00 35 34 0F 0F " +
                          "02 08 00 04 00 02 01 00 34 0F 00 0C 00 00 00 00 " +
                          "00 00 02 00 30 35 03 00 00 00 00 00 00 02 04 00 " +
                          "34 30 36 02 00 00 00 00 00 00 00 00 0D 36 0D 0B";// +
                         // "00 00 00 00 00 00 00 00 04 08 00 04 00 00 00 00";// +
                          //"00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
			String map2 = "26 1B 0A 20 06 80 10 FE 00 02 30 EF 24 02 00 05 " +  //this is at D24C70
						  "07 04 25 20 20 63 46 0E 00 24 1C A0 C6 6C 0B 03 " +  //found in rom data at 124C70
						  "DE 07 0E 04 36 20 26 24 0B 1A 71 13 84 63 95 1E";

			System.out.println(uwnh.reportFound(map2));

			int nContinents = 4;
			int nIslands = 5;
			
			//System.out.println("Generating map: continents=" + nContinents + " islands=" + nIslands);
		
			System.out.println("decompression of d1c27e");  //was d1cc7e
			//ArrayList<Byte> btsc = comp.gameDecompress(1163902);
			//System.out.println(comp.dataSize + " bytes was decompressed into " + btsc.size() + " bytes.");
			
			System.out.println("\n\nDecompression of Lisbon port");  //1a8898
			//btsc = comp.gameDecompress(1738904);
			//System.out.println(comp.dataSize + " bytes was decompressed into " + btsc.size() + " bytes.");
			
			//GameMap gm = new GameMap();
			//gm.decompressOrigMap(uwnh);
			//gm.makeMap(3, 10, 4);
			System.out.println("map decompression");
			System.out.println("-----------------------");
			System.out.println("TESTING COMPRESSION");
			System.out.println("-----------------------");
			//testCompress(uwnh);
			//viewPort(uwnh);
			//testAllPorts(uwnh);
			//getPortFindData(uwnh);
			testRomDump(uwnh);
			
			//gm.decompressOrigMap(uwnh);
			//gm.makeMap(nContinents, nIslands, 0);
			//testControlCompression(uwnh);
			//testCompress2(uwnh);
			//testCompress3(uwnh);
			//boolean tested = false;
			//while(!tested)
				//tested = testMapgen(uwnh);
			/*GameMapWindow gmw = new GameMapWindow(gm);
			GameMiniMap gmm = new GameMiniMap(uwnh);
			gmm.createMiniMap(gm);
			MiniMapView mmv = new MiniMapView(gmm);
			gmm.testCompressedBytes(uwnh, gm);*/
			
			//compression savings: 
			//Discovery text @967054: 13240->8921 (67.38%, 4319 net)
			//Discovery names @980597: 1186->986 (83.13%, 220 net)
			//Commodity names @725394: 530->484 (91.32%, 46 net)
			//Item text @384106:  8267->5565 (67.31%, 2602 net)
			//Misc. text 1 @744327: 41278->29337 (71.07%, 11941 net)
			//Misc. text 2 @941171: 25587->18112 (70.79%, 7475 net)
			//Misc. text 3 @1013319: 16237->11729 (72.24%, 4508 net)
			//total savings: 31111 bytes
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
