package org.diskproject.client;

import org.diskproject.client.components.searchpanel.SearchableItem;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;

import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.Comparator;
import java.util.Date;

public class Utils {
	public static Comparator<TriggeredLOI> tloiSorter = new Comparator<TriggeredLOI>(){
		public int compare (TriggeredLOI l, TriggeredLOI r) {
			Status ls = l.getStatus();
			Status rs = r.getStatus();
			if (ls == null) return -1;
			if (rs == null) return 1;
			return 0;
		}
	};
	
	public static Comparator<TreeItem> ascDateOrder = new Comparator<TreeItem>() {
		public int compare (TreeItem l, TreeItem r) {
			String lc = l.getCreationDate();
			String lr = r.getCreationDate();
			if (lc != null && lr != null) {
				DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
				Date dl = fm.parse(lc);
				Date dr = fm.parse(lr);
				if (dl.after(dr)) return -1;
				else return 1;
			} else if (lc != null) {
				return -1;
			} else if (lr != null) {
				return 1;
			}
			return 0;
		}
	};

	public static Comparator<TreeItem> descDateOrder = new Comparator<TreeItem>() {
		public int compare (TreeItem l, TreeItem r) {
			String lc = l.getCreationDate();
			String lr = r.getCreationDate();
			if (lc != null && lr != null) {
				DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
				Date dl = fm.parse(lc);
				Date dr = fm.parse(lr);
				if (dl.before(dr)) return -1;
				else return 1;
			} else if (lc != null) {
				return -1;
			} else if (lr != null) {
				return 1;
			}
			return 0;
		}
	};

	public static Comparator<SearchableItem> orderAscDate = new Comparator<SearchableItem>() {
		public int compare (SearchableItem l, SearchableItem r) {
			String lc = l.getCreationDate();
			String lr = r.getCreationDate();
			if (lc != null && lr != null) {
				DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
				Date dl = fm.parse(lc);
				Date dr = fm.parse(lr);
				if (dl.after(dr)) return -1;
				else return 1;
			} else if (lc != null) {
				return -1;
			} else if (lr != null) {
				return 1;
			}
			return 0;
		}
	};

	public static Comparator<SearchableItem> orderDesDate = new Comparator<SearchableItem>() {
		public int compare (SearchableItem l, SearchableItem r) {
			String lc = l.getCreationDate();
			String lr = r.getCreationDate();
			if (lc != null && lr != null) {
				DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
				Date dl = fm.parse(lc);
				Date dr = fm.parse(lr);
				if (dl.before(dr)) return -1;
				else return 1;
			} else if (lc != null) {
				return -1;
			} else if (lr != null) {
				return 1;
			}
			return 0;
		}
	};

	public static Comparator<TriggeredLOI> orderTLOIs = new Comparator<TriggeredLOI>() {
		public int compare (TriggeredLOI l, TriggeredLOI r) {
			String lc = l.getDateCreated();
			String lr = r.getDateCreated();
			if (lc != null && lr != null) {
				DateTimeFormat fm = DateTimeFormat.getFormat("HH:mm:ss yyyy-MM-dd");
				Date dl = fm.parse(lc);
				Date dr = fm.parse(lr);
				if (dl.before(dr)) return -1;
				else return 1;
			} else if (lc != null) {
				return -1;
			} else if (lr != null) {
				return 1;
			}
			return 0;
		}
	};
	
	public static Comparator<TreeItem> ascAuthorOrder = new Comparator<TreeItem>(){
		public int compare (TreeItem l, TreeItem r) {
			String la = l.getAuthor();
			String ra = r.getAuthor();
			if (la != null && ra != null) {
				return la.compareTo(ra);
			} else if (la != null) {
				return -1;
			} else if (ra != null) {
				return 1;
			}
			return 0;
		}
	};

	public static Comparator<TreeItem> descAuthorOrder = new Comparator<TreeItem>(){
		public int compare (TreeItem l, TreeItem r) {
			String la = l.getAuthor();
			String ra = r.getAuthor();
			if (la != null && ra != null) {
				return ra.compareTo(la);
			} else if (la != null) {
				return -1;
			} else if (ra != null) {
				return 1;
			}
			return 0;
		}
	};
	
	public static String extractPrefix (String URI) {
		return extractPrefix(URI, "#");
	}
	
	public static String extractPrefix (String URI, String prefix) {
		String[] sp = URI.split(prefix);
		if (sp != null && sp.length > 0) {
			return sp[sp.length-1];
		} else {
			return URI;
		}
	}
}