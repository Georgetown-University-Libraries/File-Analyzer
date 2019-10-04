package edu.georgetown.library.fileAnalyzer.filetest;

import gov.nara.nwts.ftapp.FTDriver;
import gov.nara.nwts.ftapp.filetest.DefaultFileTest;
import gov.nara.nwts.ftapp.filter.PdfFileTestFilter;
import gov.nara.nwts.ftapp.ftprop.FTPropString;
import gov.nara.nwts.ftapp.stats.Stats;
import gov.nara.nwts.ftapp.stats.StatsGenerator;
import gov.nara.nwts.ftapp.stats.StatsItem;
import gov.nara.nwts.ftapp.stats.StatsItemConfig;
import gov.nara.nwts.ftapp.stats.StatsItemEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extract all metadata fields from a TIF or JPG using categorized tag defintions.
 * @author TBrady
 *
 */
class PageCount extends DefaultFileTest { 
	private static enum PagesStatsItems implements StatsItemEnum {
		Key(StatsItem.makeStringStatsItem("Path", 200)),
		Pages(StatsItem.makeIntStatsItem("Pages")),
		PdfVer(StatsItem.makeStringStatsItem("PdfVer"))
		;
		
		StatsItem si;
		PagesStatsItems(StatsItem si) {this.si=si;}
		public StatsItem si() {return si;}
	}

	public static enum Generator implements StatsGenerator {
		INSTANCE;
		class PagesStats extends Stats {
			public PagesStats(String key) {
				super(details, key);
			}

		}
		public PagesStats create(String key) {return new PagesStats(key);}
	}
	public static StatsItemConfig details = StatsItemConfig.create(PagesStatsItems.class);

	long counter = 1000000;
	public static final String KEYPATT = "key-regex";
	public static final String KEYGROUP = "regex-group-num";
	public PageCount(FTDriver dt) {
		super(dt);
		ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  KEYPATT, KEYPATT,
				"Regular expression for setting a key: '.*(\\d\\d\\d).pdf '.  If not set, the full path name will be used", ""));
		ftprops.add(new FTPropString(dt, this.getClass().getSimpleName(),  KEYGROUP, KEYGROUP,
				"Regular expression match group number for key.  Only applicable if a regular expression is set.", "0"));
	}

	public String toString() {
		return "Page Count";
	}
	public String getKey(File f) {
		String key = f.getPath();
		
		String patt = getProperty(KEYPATT, "").toString();
		String group = getProperty(KEYGROUP, "").toString();
		if (patt.isEmpty()) return key;
		if (group.isEmpty()) return key;
		
		try {
			Pattern p = Pattern.compile(patt);
			Matcher m = p.matcher(key);
			if (m.matches()) {
				int pos = Integer.parseInt(group);
				if (pos >= 0 && pos <= m.groupCount()) {
					return m.group(pos);
				}
			}
		} catch (Exception e) {
		}
		
		return key;
	}
	
    public String getShortName(){return "Pg";}

    
	public Object fileTest(File f) {
		Stats s = getStats(f);
		int x = 0;
		try (FileInputStream fis = new FileInputStream(f)){
		    AutoDetectParser pp = new AutoDetectParser();
			Metadata m = new Metadata();
			pp.parse(fis, new DefaultHandler(), m, new ParseContext());
			try {
                x = Integer.parseInt(m.get("xmpTPg:NPages"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
			s.setVal(PagesStatsItems.Pages, x);
			s.setVal(PagesStatsItems.PdfVer, m.get("pdf:PDFVersion"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (SAXException e) {
            e.printStackTrace();
        } catch (TikaException e) {
            e.printStackTrace();
        }
		
		return x;
	}
    public Stats createStats(String key){ 
    	return Generator.INSTANCE.create(key);
    }
    public StatsItemConfig getStatsDetails() {
    	return details; 
    }

	public void initFilters() {
		filters.add(new PdfFileTestFilter());
	}

	public String getDescription() {
		return "";
	}

}
