package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.StorageQuotaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class StorageBudgetTest {
    @TempDir Path directory;
    private StorageBudget budget(long user, long total, long free) {
        return new StorageBudget(directory.toString(), new StorageQuotaProperties(user, total, free, 2));
    }
    private void stored(UUID owner, UUID id, int size) throws Exception {
        Files.write(directory.resolve(id+".img"), new byte[size]);
        Files.writeString(directory.resolve(id+".properties"), "owner="+owner+"\n");
    }
    @Test
    void restartReconcilesExistingOwnerFilesAndThumbnails() throws Exception {
        UUID owner = UUID.randomUUID(), id = UUID.randomUUID();
        stored(owner,id,500); Files.write(directory.resolve(id+".thumb"),new byte[200]);
        assertThatThrownBy(() -> budget(800,10000,0).write(owner,200,()->{throw new AssertionError("must not write");}))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getCode()).isEqualTo("IMAGE_STORAGE_QUOTA"));
        assertThat(budget(800,10000,0).write(UUID.randomUUID(),200,()->true)).isTrue();
    }
    @Test
    void globalQuotaIncludesOrphanFilesAndKeepsExistingFilesReadable() throws Exception {
        Path orphan=directory.resolve("orphan.img"); Files.write(orphan,new byte[500]);
        assertThatThrownBy(()->budget(10000,600,0).write(UUID.randomUUID(),200,()->true)).isInstanceOf(BusinessException.class);
        assertThat(Files.size(orphan)).isEqualTo(500);
    }
    @Test
    void freeSpaceReserveRejectsBeforeWriting() {
        assertThatThrownBy(()->budget(10000,10000,Long.MAX_VALUE).write(UUID.randomUUID(),1,()->true)).isInstanceOf(BusinessException.class);
    }
    @Test
    void concurrentUploadsCannotRacePastAUserQuota() throws Exception {
        UUID owner=UUID.randomUUID(); var budget=budget(1200,10000,0);
        try(var pool=Executors.newFixedThreadPool(8)) {
            var tasks=new ArrayList<Callable<Boolean>>();
            for(int i=0;i<8;i++)tasks.add(()->{
                try {return budget.write(owner,300,()->{try {stored(owner,UUID.randomUUID(),200);} catch(Exception e){throw new java.io.IOException(e);} return true;});}
                catch(BusinessException quota){return false;}
            });
            var results=pool.invokeAll(tasks); int accepted=0;
            for(var result:results)if(result.get())accepted++;
            assertThat(accepted).isBetween(1,4);
            try(var files=Files.list(directory)) { assertThat(files.filter(p->p.toString().endsWith(".img")).count()).isEqualTo(accepted); }
        }
    }
}
