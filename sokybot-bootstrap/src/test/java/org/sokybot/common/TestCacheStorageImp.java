package org.sokybot.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.common.mapper.Mappable;
import org.dizitart.no2.common.mapper.NitriteMapper;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.repository.ObjectRepository;
import org.sokybot.ICacheStorage;
import org.sokybot.app.AppConstants;

public class TestCacheStorageImp implements ICacheStorage {

	public static final String STORAGE_META = "storage-meta";

	private Nitrite db;

	private Document keys;
	private Map<String, DataEntryList> types = new HashMap<>();

	private NitriteCollection metaColl;

	public TestCacheStorageImp(Nitrite db) {
		this.db = db;

		this.metaColl = db.getCollection(AppConstants.DB_META);
		Document doc = this.metaColl.find(FluentFilter.where(STORAGE_META).notEq(null)).firstOrNull();

		if (doc == null) {

			doc = Document.createDocument().put(STORAGE_META, true);
			this.metaColl.update(doc, true);
		}
		this.keys = doc;

	}

	private DataEntryList getDL(String type, String key) {
		DataEntryList dl = null;
		if (this.types.containsKey(type)) {
			dl = this.types.get(type);

			if (dl.contains(key)) {
				return dl;
			}

		}

		// search db

		String typeKey = type + key;
		typeKey = typeKey.replace('.', '-');

		String dlId = this.keys.get(typeKey, String.class);

		ObjectRepository<DataEntryList> repo = this.db.getRepository(DataEntryList.class, type);

		dl = this.types.remove(type);

		if (dl != null) {
			repo.update(dl, false);
			repo.getStore().commit();

			System.out.println("save old DataEntryList") ;
		}

		dl = repo.getById(dlId);

		if (dl == null)
			throw new IllegalStateException(
					"Could not find target DataEntryList for key " + typeKey + " with id " + dlId);

		if (!dl.contains(key))
			throw new IllegalStateException("Invalid meta stored into keys document");

		this.types.put(type, dl);

		return dl;

	}

	private DataEntryList save(String key, Object val) {

		DataEntryList dl = null;

		String type = val.getClass().getTypeName();
		// check dl if is not full then put value else remove it from memory and create
		// new one
		if (this.types.containsKey(type)) {
			dl = this.types.get(type);
			if (!dl.isFull()) {
				dl.put(key, val);
			} else {
				// update this object
				ObjectRepository<DataEntryList> repo = this.db.getRepository(DataEntryList.class, type);

				repo.update(dl, true);
				this.types.remove(type, dl);
				System.out.println("Insert DataEntryList " + dl.getId());

				dl = new DataEntryList(type);
				repo.update(dl, true);

				dl.put(key, val);
				this.types.put(type, dl);
				

			}

		} else {

			// check db if there exists empty dl then put it into memory else create new one
			// and save it

			ObjectRepository<DataEntryList> repo = this.db.getRepository(DataEntryList.class, type);
			Iterator<DataEntryList> ite = repo.find().iterator();

			while (ite.hasNext()) {
				dl = ite.next();
				if (!dl.isFull()) {
					this.types.put(type, dl);
					dl.put(key, val);
					return dl;
				}
			}

			dl = new DataEntryList(type);

			repo.update(dl, true);
			dl.put(key, val);
			this.types.put(type, dl);
		}

		return dl;
	}

	@Override
	public void store(String key, Object val) {

		String type = val.getClass().getTypeName();

		if (keys.containsKey((type + key).replace('.', '-'))) { // override
			 System.out.println("Override " + type + key);
			DataEntryList dl = getDL(type, key);
			dl.put(key, val);
			// System.out.println("Target DataEntryList : " + dl) ;
		} else { // store
			// System.out.println("Save " + type + key);
			// store and save key
			DataEntryList dl = save(key, val);

			String dlId = dl.getId();

			// System.out.println("Save DataEntryList with id " + dlId);
			this.keys.put((type + key).replace('.', '-'), dlId);
		}
	}

	@Override
	public <T> T getValueOrDefault(String key, Class<T> type, T val) {

		T v = getValue(key, type);
		return v == null ? val : v;
	}

	@Override
	public <T> T getValue(String key, Class<T> type) {

		String typeName = type.getTypeName();
		String typeKey = typeName + key;
		typeKey = typeKey.replace('.', '-');

		if (keys.containsKey(typeKey)) {
			DataEntryList dl = this.types.get(typeName);
			if (dl != null && dl.contains(key)) {
				return dl.getValue(key, type);
			} else {

				ObjectRepository<DataEntryList> repo = this.db.getRepository(DataEntryList.class, typeName);

				String dlId = this.keys.get(typeKey, String.class);

				dl = repo.getById(dlId);
				if (dl.contains(key)) {
					DataEntryList old = this.types.get(typeName);
					if (old != null) {
						repo.update(old, true);

					}
					this.types.put(typeName, dl);
					return dl.getValue(key, type);
				} else {
					// throw illegal state exception
					throw new IllegalStateException("Could not find entry { " + key + " } at its expected repo " + dl);
				}

			}
		}

		return null;

	}

	@Override
	public <T> List<T> getAll(Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		this.types.forEach((k, v) -> {
			this.db.getRepository(DataEntryList.class, k)
			
			.update(v, false);
		
			this.db.commit();
			System.out.println("DataEntryList of type : " + k + " has size " + v.valueList.size());
		});

		this.metaColl.update(keys, false);
		// if (doc == null) {
		// throw new IllegalStateException("Could not find keys document");
		// } else {

		// coll.update(doc , true);
		// }

		boolean  hasChange  = this.db.getStore().hasUnsavedChanges() ; 
		if(hasChange) { 
			System.out.println("Store has change to commit") ;
		}else { 
			System.out.println("Store does not have changes to commit") ; 
		}
		
		this.types.clear();
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	public static class DataEntryList implements Mappable, Serializable {
		
	//	@Id
		private  String id;

		// private Map<String, Object> valueList =new HashMap<>() ;

		private transient Document valueList;

		// private Map<String, Object> valueList ;

		private int capcity;
		private String type;

		public DataEntryList() {
			System.out.println("DataEntryList Created using default constractor") ;
		}

		public DataEntryList(String type) {

			this.type = type;

			this.id = NitriteId.newId().getIdValue();
			System.out.println("DataEntryList Creating using Type Constractor where id was " + this.id) ; 
		}

		@Override
		public String toString() {
			return "DataEntryList [id=" + id + ", valueList=" + valueList + ", capcity=" + capcity + ", type=" + type
					+ "]";
		}

		@Override
		public void read(NitriteMapper mapper, Document document) {

			System.out.println("Convert Document to DataEntryList");
			this.valueList = document;
			this.id = this.valueList.get("_id", String.class);

			this.capcity = this.valueList.get("caps", Integer.class);
			this.type = this.valueList.get("type", String.class);
			System.out.println("Load doc for type " + this.type + " , Caps , " + this.capcity + "Size : "
					+ this.valueList.size() + " , id " + this.id);
		}

		@Override
		public Document write(NitriteMapper mapper) {
			System.out.println("Convert DataEntryList to Document");
			if (this.valueList == null) {
				System.out.println("Save new doc");
				System.out.println("The Generated id is : " + this.id) ; 
				this.valueList = Document.createDocument();
				this.valueList.put("_id", this.id);
				
				this.valueList.put("id",this.id) ; 
				this.valueList.put("type", this.type);
				this.capcity = 50000;

				// if(type.startsWith("java.lang")) {
				// this.capcity = 100000 ;
				// }
				this.valueList.put("caps", this.capcity);

			}

			// System.out.println("Write Doc for type " + this.type + " , id " + this.id );
			// this.valueList.put("id", this.id) ;

			return this.valueList;
		}

		public boolean isFull() {
			return this.valueList.size() >= this.capcity;
		}

		public boolean isEmpty() {
			return this.valueList.size() < this.capcity;
		}

		public boolean contains(String key) {
			return this.valueList.containsKey(key);
		}

		public String getId() {
			return this.id;
		}

		public <T> T getValue(String key, Class<T> type) {

			Object obj = valueList.get(key);

			if (obj != null && type.isInstance(obj)) {
				return type.cast(obj);
			} else {

				throw new IllegalStateException("Object " + obj + " of type " + obj.getClass().getTypeName()
						+ " stored into inappropriate list contains objects of " + this.type);
			}

		}

		public <T> void put(String key, T value) {
			this.valueList.put(key, value);
		}

		public <T> List<T> toList(Class<T> type) {

			List<T> res = new ArrayList<>();

			this.valueList.forEach((p) -> {
				res.add(type.cast(p.getSecond()));
			});
			return res;

			// return this.valueList.values().stream()
			// .filter((v)->type.isInstance(v))
			// .map((v)->type.cast(v))
			// .toList();

		}

		public static void main(String args[]) {

			 TestCacheStoreData d = new TestCacheStoreData() ;
			 d.testAddData();

		//	Nitrite db = db();

		//	ObjectRepository<DataEntryList> repo = db.getRepository(DataEntryList.class, Segment.class.getTypeName());

		//	repo.find().forEach((del) -> {
		//		System.out.println(del.getId());
		//	});
		//	;

		}

	}

}
