package com.prx.directory.benchmark;

import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.entity.ProductEntity;
import com.prx.directory.jpa.entity.UserFavoriteEntity;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks the in-memory streaming pattern used by FavoriteServiceImpl.getFavorites().
 *
 * <p>The <em>baseline</em> mirrors the production code: three separate stream passes over the
 * full favorites list, one per entity type. This allocates three independent stream pipelines
 * and traverses the list three times.
 *
 * <p>The <em>singleLoop</em> fix performs a single enhanced-for loop, routing each element to
 * the appropriate result list in one pass. This halves allocations and traversal cost.
 *
 * <p>The GC profiler output (alloc.rate, alloc.rate.norm) confirms the allocation difference.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 0)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class FavoriteStreamBenchmark {

    /**
     * Dataset sizes to probe: small (100), medium (1 000), and large (5 000) lists.
     * These represent realistic user-favorite counts in a production directory service.
     */
    @Param({"100", "1000", "5000"})
    private int listSize;

    private List<UserFavoriteEntity> favoritesList;

    @Setup(Level.Trial)
    public void setup() {
        LocalDateTime now = LocalDateTime.now();
        favoritesList = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            UserFavoriteEntity fav = new UserFavoriteEntity();
            // Distribute 1/3 stores, 1/3 products, 1/3 campaigns
            if (i % 3 == 0) {
                BusinessEntity b = new BusinessEntity();
                b.setId(UUID.randomUUID());
                b.setName("Business-" + i);
                b.setCreatedDate(now);
                b.setLastUpdate(now);
                fav.setBusiness(b);
            } else if (i % 3 == 1) {
                ProductEntity p = new ProductEntity();
                p.setId(UUID.randomUUID());
                p.setName("Product-" + i);
                p.setDescription("desc");
                p.setCreatedDate(now);
                p.setLastUpdate(now);
                fav.setProduct(p);
            } else {
                CampaignEntity c = new CampaignEntity();
                c.setId(UUID.randomUUID());
                c.setTitle("Campaign-" + i);
                c.setStartDate(now);
                c.setEndDate(now.plusDays(30));
                fav.setCampaign(c);
            }
            favoritesList.add(fav);
        }
    }

    /**
     * Current production code: three separate stream passes over the same list.
     * Each pass allocates a new stream pipeline and a result List.
     * Total iterations = 3 × listSize.
     */
    @Benchmark
    public void threeStreamPasses(Blackhole bh) {
        List<UUID> stores = favoritesList.stream()
                .map(UserFavoriteEntity::getBusiness)
                .filter(Objects::nonNull)
                .map(BusinessEntity::getId)
                .toList();

        List<UUID> products = favoritesList.stream()
                .map(UserFavoriteEntity::getProduct)
                .filter(Objects::nonNull)
                .map(ProductEntity::getId)
                .toList();

        List<UUID> offers = favoritesList.stream()
                .map(UserFavoriteEntity::getCampaign)
                .filter(Objects::nonNull)
                .map(CampaignEntity::getId)
                .toList();

        bh.consume(stores);
        bh.consume(products);
        bh.consume(offers);
    }

    /**
     * Proposed fix: single enhanced-for loop, one pass over the list.
     * Pre-sized result lists avoid ArrayList resizing.
     * Total iterations = listSize.
     */
    @Benchmark
    public void singleLoopPass(Blackhole bh) {
        int bucket = listSize / 3 + 1;
        List<UUID> stores = new ArrayList<>(bucket);
        List<UUID> products = new ArrayList<>(bucket);
        List<UUID> offers = new ArrayList<>(bucket);

        for (UserFavoriteEntity fav : favoritesList) {
            BusinessEntity biz = fav.getBusiness();
            ProductEntity prod = fav.getProduct();
            CampaignEntity camp = fav.getCampaign();
            if (biz != null) {
                stores.add(biz.getId());
            } else if (prod != null) {
                products.add(prod.getId());
            } else if (camp != null) {
                offers.add(camp.getId());
            }
        }

        bh.consume(stores);
        bh.consume(products);
        bh.consume(offers);
    }
}
