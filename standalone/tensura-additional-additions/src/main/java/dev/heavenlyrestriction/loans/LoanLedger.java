package dev.heavenlyrestriction.loans;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
/** One authoritative world ledger. Time uses saved server ticks, never client clocks. */
public final class LoanLedger extends SavedData {
    public static final class Loan {
        public UUID id,lender,borrower;public String skill,lenderName,borrowerName,status="Open",stolen="";
        public long created,lastUsed,used,lastGap,longestGap;public double earned;public boolean wasActive;
        public CompoundTag save(){var t=new CompoundTag();t.putUUID("id",id);t.putUUID("lender",lender);t.putUUID("borrower",borrower);t.putString("skill",skill);t.putString("lenderName",lenderName);t.putString("borrowerName",borrowerName);t.putString("status",status);t.putString("stolen",stolen);t.putLong("created",created);t.putLong("lastUsed",lastUsed);t.putLong("used",used);t.putLong("lastGap",lastGap);t.putLong("longestGap",longestGap);t.putDouble("earned",earned);t.putBoolean("wasActive",wasActive);return t;}
        static Loan read(CompoundTag t){var l=new Loan();l.id=t.getUUID("id");l.lender=t.getUUID("lender");l.borrower=t.getUUID("borrower");l.skill=t.getString("skill");l.lenderName=t.getString("lenderName");l.borrowerName=t.getString("borrowerName");l.status=t.getString("status");l.stolen=t.getString("stolen");l.created=t.getLong("created");l.lastUsed=t.getLong("lastUsed");l.used=t.getLong("used");l.lastGap=t.getLong("lastGap");l.longestGap=t.getLong("longestGap");l.earned=t.getDouble("earned");l.wasActive=t.getBoolean("wasActive");return l;}
        public boolean open(){return status.equals("Open");}
    }
    public final Map<UUID,Loan> loans=new LinkedHashMap<>();public final Map<UUID,Double> pending=new HashMap<>();public long clock;
    private final Map<UUID,Loan> active=new LinkedHashMap<>();
    public Collection<Loan> active(){return active.values();}
    public void closed(Loan l){active.remove(l.id);var list=borrowers.get(l.borrower);if(list!=null){list.remove(l);if(list.isEmpty())borrowers.remove(l.borrower);}setDirty();}
    private final Map<UUID,List<Loan>> borrowers=new HashMap<>();
    public List<Loan> forBorrower(UUID id){return borrowers.getOrDefault(id,List.of());}
    public void add(Loan l){loans.put(l.id,l);if(l.open()){active.put(l.id,l);borrowers.computeIfAbsent(l.borrower,k->new ArrayList<>()).add(l);}setDirty();}
    public static LoanLedger get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(LoanLedger::new,LoanLedger::load),"heavenlyrestriction_loan_shark");}
    public static LoanLedger load(CompoundTag tag,HolderLookup.Provider lookup){var d=new LoanLedger();d.clock=Math.max(0,tag.getLong("clock"));for(var raw:tag.getList("loans",Tag.TAG_COMPOUND)){try{d.add(Loan.read((CompoundTag)raw));}catch(RuntimeException ignored){}}for(var raw:tag.getList("pending",Tag.TAG_COMPOUND)){var t=(CompoundTag)raw;double amount=t.getDouble("ep");if(t.hasUUID("owner") && Double.isFinite(amount) && amount>0)d.pending.put(t.getUUID("owner"),amount);}return d;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.putLong("clock",clock);var list=new ListTag();for(var loan:loans.values())list.add(loan.save());tag.put("loans",list);var credits=new ListTag();pending.forEach((id,ep)->{var t=new CompoundTag();t.putUUID("owner",id);t.putDouble("ep",ep);credits.add(t);});tag.put("pending",credits);return tag;}
}
