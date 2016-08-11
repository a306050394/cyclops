package com.aol.cyclops.control;

import static com.aol.cyclops.control.Matchable.otherwise;
import static com.aol.cyclops.control.Matchable.then;
import static com.aol.cyclops.control.Matchable.when;
import static com.aol.cyclops.util.function.Predicates.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.junit.Before;
import org.junit.Test;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.Reducers;
import com.aol.cyclops.Semigroups;
import com.aol.cyclops.data.LazyImmutable;
import com.aol.cyclops.data.Mutable;
import com.aol.cyclops.data.collections.extensions.persistent.PBagX;
import com.aol.cyclops.data.collections.extensions.persistent.POrderedSetX;
import com.aol.cyclops.data.collections.extensions.persistent.PQueueX;
import com.aol.cyclops.data.collections.extensions.persistent.PSetX;
import com.aol.cyclops.data.collections.extensions.persistent.PStackX;
import com.aol.cyclops.data.collections.extensions.persistent.PVectorX;
import com.aol.cyclops.data.collections.extensions.standard.DequeX;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.data.collections.extensions.standard.QueueX;
import com.aol.cyclops.data.collections.extensions.standard.SetX;
import com.aol.cyclops.data.collections.extensions.standard.SortedSetX;
import com.aol.cyclops.types.applicative.ApplicativeFunctor.Applicatives;
import com.aol.cyclops.util.function.Predicates;
import com.aol.cyclops.util.stream.StreamUtils;



public class Xor2Test {

	Xor<String,Integer> just;
	Xor<String,Integer> none;
	@Before
	public void setUp() throws Exception {
		just = Xor.primary(10);
		none = Xor.secondary("none");
	}
	@Test
    public void testApFeatureToggle() {
      
        assertThat(just.combine(FeatureToggle.enable(20),this::add).get(),equalTo(30));
    }
   
	@Test
    public void testZip(){
        assertThat(Xor.primary(10).zip(Eval.now(20),(a,b)->a+b).get(),equalTo(30));
        assertThat(Xor.primary(10).zip((a,b)->a+b,Eval.now(20)).get(),equalTo(30));
        assertThat(Xor.primary(10).zip(Stream.of(20),(a,b)->a+b).get(),equalTo(30));
        assertThat(Xor.primary(10).zip(Seq.of(20),(a,b)->a+b).get(),equalTo(30));
        assertThat(Xor.primary(10).zip(Seq.of(20)).get(),equalTo(Tuple.tuple(10,20)));
        assertThat(Xor.primary(10).zip(Stream.of(20)).get(),equalTo(Tuple.tuple(10,20)));
        assertThat(Xor.primary(10).zip(Eval.now(20)).get(),equalTo(Tuple.tuple(10,20)));
    }
   

    @Test
    public void testZipPubFeatureToggle() {
        assertThat(just.zip(FeatureToggle.enable(20),this::add).get(),equalTo(30));
    }
	@Test
    public void nest(){
       assertThat(just.nest().map(m->m.get()),equalTo(just));
       assertThat(none.nest().map(m->m.get()),equalTo(none));
    }
    @Test
    public void coFlatMap(){
        assertThat(just.coflatMap(m-> m.isPresent()? m.get() : 50),equalTo(just));
        assertThat(none.coflatMap(m-> m.isPresent()? m.get() : 50),equalTo(Xor.primary(50)));
    }
    @Test
    public void combine(){
        Monoid<Integer> add = Monoid.of(0,Semigroups.intSum);
        assertThat(just.combineEager(add,none),equalTo(Xor.primary(10)));
        assertThat(none.combineEager(add,just),equalTo(Xor.primary(0))); 
        assertThat(none.combineEager(add,none),equalTo(Xor.primary(0))); 
        assertThat(just.combineEager(add,Xor.primary(10)),equalTo(Xor.primary(20)));
        Monoid<Integer> firstNonNull = Monoid.of(null , Semigroups.firstNonNull());
        assertThat(just.combineEager(firstNonNull,Xor.primary(null)),equalTo(just));
         
    }
	@Test
	public void visit(){
	    
	    assertThat(just.visit(secondary->"no", primary->"yes"),equalTo("yes"));
	    assertThat(none.visit(secondary->"no", primary->"yes"),equalTo("no"));
	}
	@Test
    public void visitXor(){
        assertThat(just.mapBoth(secondary->"no", primary->"yes"),equalTo(Xor.primary("yes")));
        assertThat(none.mapBoth(secondary->"no", primary->"yes"),equalTo(Xor.secondary("no")));
    }
	@Test
	public void testToMaybe() {
		assertThat(just.toMaybe(),equalTo(Maybe.of(10)));
		assertThat(none.toMaybe(),equalTo(Maybe.none()));
	}

	private int add1(int i){
		return i+1;
	}
	@Test
	public void testApplicativeBuilder() {
		assertThat(Applicatives.<Integer,Integer>applicatives(just, just)
					.applicative(this::add1).ap(Optional.of(20)).get(),equalTo(21));
	}

	

	

	
	@Test
	public void testOfT() {
		assertThat(Ior.primary(1),equalTo(Ior.primary(1)));
	}

	

	

	

	@Test
	public void testSequence() {
		Xor<ListX<String>,ListX<Integer>> maybes =Xor.sequencePrimary(ListX.of(just,none,Xor.primary(1)));
		assertThat(maybes,equalTo(Xor.primary(ListX.of(10,1))));
	}

	@Test
	public void testAccumulateJustCollectionXOfMaybeOfTReducerOfR() {
		Xor<?,PSetX<Integer>> maybes =Xor.accumulatePrimary(ListX.of(just,none,Xor.primary(1)),Reducers.toPSetX());
		assertThat(maybes,equalTo(Xor.primary(PSetX.of(10,1))));
	}

	@Test
	public void testAccumulateJustCollectionXOfMaybeOfTFunctionOfQsuperTRSemigroupOfR() {
		Xor<?,String> maybes = Xor.accumulatePrimary(ListX.of(just,none,Xor.primary(1)),i->""+i,Semigroups.stringConcat);
		assertThat(maybes,equalTo(Xor.primary("101")));
	}
	@Test
	public void testAccumulateJust() {
		Xor<?,Integer> maybes =Xor.accumulatePrimary(ListX.of(just,none,Xor.primary(1)),Semigroups.intSum);
		assertThat(maybes,equalTo(Xor.primary(11)));
	}
	@Test
    public void testAccumulateSecondary() {
        Xor<?,String> maybes =Xor.accumulateSecondary(ListX.of(just,none,Xor.secondary("hello")),Semigroups.stringConcat);
        assertThat(maybes,equalTo(Xor.primary("nonehello")));
    }

	@Test
	public void testUnitT() {
		assertThat(just.unit(20),equalTo(Xor.primary(20)));
	}

	

	@Test
	public void testisPrimary() {
		assertTrue(just.isPrimary());
		assertFalse(none.isPrimary());
	}

	
	@Test
	public void testMapFunctionOfQsuperTQextendsR() {
		assertThat(just.map(i->i+5),equalTo(Xor.primary(15)));
		assertThat(none.map(i->i+5),equalTo(Xor.secondary("none")));
	}

	@Test
	public void testFlatMap() {
		assertThat(just.flatMap(i->Xor.primary(i+5)),equalTo(Xor.primary(15)));
		assertThat(none.flatMap(i->Xor.primary(i+5)),equalTo(Xor.secondary("none")));
	}

	@Test
	public void testWhenFunctionOfQsuperTQextendsRSupplierOfQextendsR() {
		assertThat(just.visit(i->i+1,()->20),equalTo(11));
		assertThat(none.visit(i->i+1,()->20),equalTo(20));
	}

	@Test
	public void testUnapply() {
		assertThat(just.unapply(),equalTo(ListX.of(10)));
		assertThat(none.unapply(),equalTo(ListX.of("none")));
	}

	@Test
	public void testStream() {
		assertThat(just.stream().toListX(),equalTo(ListX.of(10)));
		assertThat(none.stream().toListX(),equalTo(ListX.of()));
	}

	@Test
	public void testOfSupplierOfT() {
		
	}

	@Test
    public void testConvertTo() {
	   
        Stream<Integer> toStream = just.visit(m->Stream.of(m),()->Stream.of());
        assertThat(toStream.collect(Collectors.toList()),equalTo(ListX.of(10)));
    }


    @Test
    public void testConvertToAsync() {
        FutureW<Stream<Integer>> async = FutureW.ofSupplier(()->just.visit(f->Stream.of((int)f),()->Stream.of()));
        
        assertThat(async.get().collect(Collectors.toList()),equalTo(ListX.of(10)));
    }
	
	@Test
	public void testIterate() {
		assertThat(just.iterate(i->i+1).limit(10).sum(),equalTo(Optional.of(145)));
	}

	@Test
	public void testGenerate() {
		assertThat(just.generate().limit(10).sum(),equalTo(Optional.of(100)));
	}

	@Test
	public void testMapReduceReducerOfE() {
		assertThat(just.mapReduce(Reducers.toCountInt()),equalTo(1));
	}

	@Test
	public void testFoldMonoidOfT() {
		assertThat(just.fold(Reducers.toTotalInt()),equalTo(10));
	}

	@Test
	public void testFoldTBinaryOperatorOfT() {
		assertThat(just.fold(1, (a,b)->a*b),equalTo(10));
	}

	@Test
	public void testToLazyImmutable() {
		assertThat(just.toLazyImmutable(),equalTo(LazyImmutable.of(10)));
	}
	@Test(expected=NoSuchElementException.class)
	public void testToLazyImmutableNone(){
		none.toLazyImmutable();
		fail("exception expected");
		
	}

	@Test
	public void testToMutable() {
		assertThat(just.toMutable(),equalTo(Mutable.of(10)));
		
		
	}
	@Test(expected=NoSuchElementException.class)
	public void testToMutableNone(){
		none.toMutable();
		fail("exception expected");
		
	}

	@Test
	public void testToXor() {
		assertThat(just.toXor(),equalTo(Xor.primary(10)));
		
	}
	@Test
	public void testToXorNone(){
		Xor<String,Integer> xor = none.toXor();
		assertTrue(xor.isSecondary());
		assertThat(xor,equalTo(Xor.secondary("none")));
		
	}


	@Test
	public void testToXorSecondary() {
		assertThat(just.toXor().swap(),equalTo(Xor.secondary(10)));
	}

	@Test
	public void testToXorSecondaryNone(){
		Xor<Integer,String> xorNone = none.toXor().swap();
		assertThat(xorNone,equalTo(Xor.primary("none")));
		
	}
	@Test
	public void testToTry() {
		assertTrue(none.toTry().isFailure());
		assertThat(just.toTry(),equalTo(Try.success(10)));
	}

	@Test
	public void testToTryClassOfXArray() {
		assertTrue(none.toTry(Throwable.class).isFailure());
	}

	@Test
	public void testToIor() {
		assertThat(just.toIor(),equalTo(Ior.primary(10)));
		
	}
	@Test
	public void testToIorNone(){
		Ior<String,Integer> ior = none.toIor();
		assertTrue(ior.isSecondary());
        assertThat(ior,equalTo(Ior.secondary("none")));
		
	}


	@Test
	public void testToIorSecondary() {
		assertThat(just.toIor().swap(),equalTo(Ior.secondary(10)));
	}
	

	@Test
	public void testToIorSecondaryNone(){
	    Ior<Integer,String> ior = none.toIor().swap();
        assertTrue(ior.isPrimary());
        assertThat(ior,equalTo(Ior.primary("none")));
		
	}
	@Test
	public void testToEvalNow() {
		assertThat(just.toEvalNow(),equalTo(Eval.now(10)));
	}
	@Test(expected=NoSuchElementException.class)
	public void testToEvalNowNone() {
		none.toEvalNow();
		fail("exception expected");
		
	}

	@Test
	public void testToEvalLater() {
		assertThat(just.toEvalLater(),equalTo(Eval.later(()->10)));
	}
	@Test(expected=NoSuchElementException.class)
	public void testToEvalLaterNone() {
		none.toEvalLater().get();
		fail("exception expected");
		
	}

	@Test
	public void testToEvalAlways() {
		assertThat(just.toEvalAlways(),equalTo(Eval.always(()->10)));
	}
	@Test(expected=NoSuchElementException.class)
	public void testToEvalAlwaysNone() {
		none.toEvalAlways().get();
		fail("exception expected");
		
	}

	@Test
	public void testToListX() {
		
		assertThat(just.toListX(),equalTo(ListX.singleton(10)));
		assertThat(none.toListX(),equalTo(ListX.empty()));
	}

	@Test
	public void testToSetX() {
		assertThat(just.toSetX(),equalTo(SetX.singleton(10)));
		assertThat(none.toSetX(),equalTo(SetX.empty()));
	}

	@Test
	public void testToSortedSetX() {
		assertThat(just.toSortedSetX(),equalTo(SortedSetX.singleton(10)));
		assertThat(none.toSortedSetX(),equalTo(SortedSetX.empty()));
	}

	@Test
	public void testToQueueX() {
		assertThat(just.toQueueX().toList(),equalTo(QueueX.singleton(10).toList()));
		assertThat(none.toQueueX().toList(),equalTo(QueueX.empty().toList()));
	}

	@Test
	public void testToDequeX() {
		assertThat(just.toDequeX().toList(),equalTo(Arrays.asList(10)));
		assertThat(none.toDequeX().toList(),equalTo(DequeX.empty().toList()));
	}

	@Test
	public void testToPStackX() {
		assertThat(just.toPStackX(),equalTo(PStackX.singleton(10)));
		assertThat(none.toPStackX(),equalTo(PStackX.empty()));
	}

	@Test
	public void testToPVectorX() {
		assertThat(just.toPVectorX(),equalTo(PVectorX.singleton(10)));
		assertThat(none.toPVectorX(),equalTo(PVectorX.empty()));
	}

	@Test
	public void testToPQueueX() {
		assertThat(just.toPQueueX().toList(),equalTo(PQueueX.singleton(10).toList()));
		assertThat(none.toPQueueX().toList(),equalTo(PQueueX.empty().toList()));
	}

	@Test
	public void testToPSetX() {
		assertThat(just.toPSetX(),equalTo(PSetX.singleton(10)));
		assertThat(none.toPSetX(),equalTo(PSetX.empty()));
	}

	@Test
	public void testToPOrderedSetX() {
		assertThat(just.toPOrderedSetX(),equalTo(POrderedSetX.singleton(10)));
		assertThat(none.toPOrderedSetX(),equalTo(POrderedSetX.empty()));
	}

	@Test
	public void testToPBagX() {
		assertThat(just.toPBagX(),equalTo(PBagX.singleton(10)));
		assertThat(none.toPBagX(),equalTo(PBagX.empty()));
	}

	@Test
	public void testMkString() {
		assertThat(just.mkString(),equalTo("Xor.primary[10]"));
		assertThat(none.mkString(),equalTo("Xor.secondary[none]"));
	}
	LazyReact react = new LazyReact();
	@Test
	public void testToFutureStreamLazyReact() {
		assertThat(just.toFutureStream(react).toList(),equalTo(Arrays.asList(10)));
		assertThat(none.toFutureStream(react).toList(),equalTo(Arrays.asList()));
	}

	@Test
	public void testToFutureStream() {
		assertThat(just.toFutureStream().toList(),equalTo(Arrays.asList(10)));
		assertThat(none.toFutureStream().toList(),equalTo(Arrays.asList()));
	}
	SimpleReact react2 = new SimpleReact();
	@Test
	public void testToSimpleReactSimpleReact() {
		assertThat(just.toSimpleReact(react2).block(),equalTo(Arrays.asList(10)));
		assertThat(none.toSimpleReact(react2).block(),equalTo(Arrays.asList()));
	}

	@Test
	public void testToSimpleReact() {
		assertThat(just.toSimpleReact().block(),equalTo(Arrays.asList(10)));
		assertThat(none.toSimpleReact().block(),equalTo(Arrays.asList()));
	}

	@Test
	public void testGet() {
		assertThat(just.get(),equalTo(10));
	}
	@Test(expected=NoSuchElementException.class)
	public void testGetNone() {
		none.get();
		
	}

	@Test
	public void testFilter() {
		assertFalse(just.filter(i->i<5).isPrimary());
		assertTrue(just.filter(i->i>5).isPrimary());
		assertFalse(none.filter(i->i<5).isPrimary());
		assertFalse(none.filter(i->i>5).isPrimary());
		
	}

	@Test
	public void testOfType() {
		assertFalse(just.ofType(String.class).isPrimary());
		assertTrue(just.ofType(Integer.class).isPrimary());
		assertFalse(none.ofType(String.class).isPrimary());
		assertFalse(none.ofType(Integer.class).isPrimary());
	}

	@Test
	public void testFilterNot() {
		assertTrue(just.filterNot(i->i<5).isPrimary());
		assertFalse(just.filterNot(i->i>5).isPrimary());
		assertFalse(none.filterNot(i->i<5).isPrimary());
		assertFalse(none.filterNot(i->i>5).isPrimary());
	}

	@Test
	public void testNotNull() {
		assertTrue(just.notNull().isPrimary());
		assertFalse(none.notNull().isPrimary());
		
	}

	


	@Test
	public void testAp1() {
		assertThat(Ior.primary(1).applyFunctions().ap1(this::add1).toMaybe(),equalTo(Ior.primary(2).toMaybe()));
	}
	
	private int add(int a, int b){
		return a+b;
	}

	@Test
	public void testAp2() {
		assertThat(Ior.primary(1).applyFunctions().ap2(this::add).ap(Optional.of(3)).toMaybe(),equalTo(Ior.primary(4).toMaybe()));
	}
	private int add3(int a, int b, int c){
		return a+b+c;
	}
	@Test
	public void testAp3() {
		assertThat(Ior.primary(1).applyFunctions().ap3(this::add3).ap(Optional.of(3)).ap(Ior.primary(4)).toMaybe(),equalTo(Ior.primary(8).toMaybe()));
	}
	private int add4(int a, int b, int c,int d){
		return a+b+c+d;
	}
	@Test
	public void testAp4() {
		assertThat(Ior.primary(1).applyFunctions().ap4(this::add4)
						.ap(Optional.of(3))
						.ap(Ior.primary(4))
						.ap(Ior.primary(6)).toMaybe(),equalTo(Ior.primary(14).toMaybe()));
	}
	private int add5(int a, int b, int c,int d,int e){
		return a+b+c+d+e;
	}
	@Test
	public void testAp5() {
		assertThat(Ior.primary(1).applyFunctions().ap5(this::add5)
				.ap(Optional.of(3))
				.ap(Ior.primary(4))
				.ap(Ior.primary(6))
				.ap(Ior.primary(10)).toMaybe(),equalTo(Ior.primary(24).toMaybe()));
	}
	@Test
    public void testAp5Secondary() {
        assertThat(Ior.<Integer,Integer>secondary(1).applyFunctions().ap5(this::add5)
                .ap(Optional.of(3))
                .ap(Ior.primary(4))
                .ap(Ior.primary(6))
                .ap(Ior.primary(10)).toMaybe(),equalTo(Ior.secondary(null).toMaybe()));
    }

	

	@Test
	public void testMapReduceReducerOfR() {
		assertThat(just.mapReduce(Reducers.toPStackX()),equalTo(just.toPStackX()));
	}

	@Test
	public void testMapReduceFunctionOfQsuperTQextendsRMonoidOfR() {
		assertThat(just.mapReduce(s->s.toString(), Monoid.of("",Semigroups.stringJoin(","))),equalTo(",10"));
	}

	@Test
	public void testReduceMonoidOfT() {
		assertThat(just.reduce(Monoid.of(1,Semigroups.intMult)),equalTo(10));
	}

	@Test
	public void testReduceBinaryOperatorOfT() {
		assertThat(just.reduce((a,b)->a+b),equalTo(Optional.of(10)));
	}

	@Test
	public void testReduceTBinaryOperatorOfT() {
		assertThat(just.reduce(10,(a,b)->a+b),equalTo(20));
	}

	@Test
	public void testReduceUBiFunctionOfUQsuperTUBinaryOperatorOfU() {
		assertThat(just.reduce(11,(a,b)->a+b,(a,b)->a*b),equalTo(21));
	}

	@Test
	public void testReduceStreamOfQextendsMonoidOfT() {
		ListX<Integer> countAndTotal = just.reduce(Stream.of(Reducers.toCountInt(),Reducers.toTotalInt()));
		assertThat(countAndTotal,equalTo(ListX.of(1,10)));
	}

	@Test
	public void testReduceIterableOfReducerOfT() {
		ListX<Integer> countAndTotal = just.reduce(Arrays.asList(Reducers.toCountInt(),Reducers.toTotalInt()));
		assertThat(countAndTotal,equalTo(ListX.of(1,10)));
	}

	

	@Test
	public void testFoldRightMonoidOfT() {
		assertThat(just.foldRight(Monoid.of(1,Semigroups.intMult)),equalTo(10));
	}

	@Test
	public void testFoldRightTBinaryOperatorOfT() {
		assertThat(just.foldRight(10,(a,b)->a+b),equalTo(20));
	}

	@Test
	public void testFoldRightMapToType() {
		assertThat(just.foldRightMapToType(Reducers.toPStackX()),equalTo(just.toPStackX()));
	}

	
	
	@Test
	public void testWhenFunctionOfQsuperMaybeOfTQextendsR() {
		assertThat(just.visit(s->"hello", ()->"world"),equalTo("hello"));
		assertThat(none.visit(s->"hello", ()->"world"),equalTo("world"));
	}

	
	@Test
	public void testOrElseGet() {
		assertThat(none.orElseGet(()->2),equalTo(2));
		assertThat(just.orElseGet(()->2),equalTo(10));
	}

	@Test
	public void testToOptional() {
		assertFalse(none.toOptional().isPresent());
		assertTrue(just.toOptional().isPresent());
		assertThat(just.toOptional(),equalTo(Optional.of(10)));
	}

	@Test
	public void testToStream() {
		assertThat(none.toStream().collect(Collectors.toList()).size(),equalTo(0));
		assertThat(just.toStream().collect(Collectors.toList()).size(),equalTo(1));
		
	}

	@Test
	public void testToAtomicReference() {
		assertThat(just.toAtomicReference().get(),equalTo(10));
	}
	@Test(expected=NoSuchElementException.class)
	public void testToAtomicReferenceNone() {
		none.toAtomicReference().get();
	}

	@Test
	public void testToOptionalAtomicReference() {
		assertFalse(none.toOptionalAtomicReference().isPresent());
		assertTrue(just.toOptionalAtomicReference().isPresent());
	}

	@Test
	public void testOrElse() {
		assertThat(none.orElse(20),equalTo(20));
		assertThat(just.orElse(20),equalTo(10));
	}

	@Test(expected=RuntimeException.class)
	public void testOrElseThrow() {
		none.orElseThrow(()->new RuntimeException());
	}
	@Test
	public void testOrElseThrowSome() {
		
		assertThat(just.orElseThrow(()->new RuntimeException()),equalTo(10));
	}

	@Test
	public void testToList() {
		assertThat(just.toList(),equalTo(Arrays.asList(10)));
		assertThat(none.toListX(),equalTo(new ArrayList<>()));
	}

	
	@Test
	public void testToFutureW() {
		FutureW<Integer> cf = just.toFutureW();
		assertThat(cf.get(),equalTo(10));
	}

	@Test
	public void testToCompletableFuture() {
		CompletableFuture<Integer> cf = just.toCompletableFuture();
		assertThat(cf.join(),equalTo(10));
	}

	@Test
	public void testToCompletableFutureAsync() {
		CompletableFuture<Integer> cf = just.toCompletableFutureAsync();
		assertThat(cf.join(),equalTo(10));
	}
	Executor exec = Executors.newFixedThreadPool(1);

	@Test
	public void testToCompletableFutureAsyncExecutor() {
		CompletableFuture<Integer> cf = just.toCompletableFutureAsync(exec);
		assertThat(cf.join(),equalTo(10));
	}

	

	@Test
    public void testMatches() {
        assertThat(just.matches(c->c.is(when("10"),then("hello")),
                                        c->c.is(when(instanceOf(Integer.class)), then("error")),
                                            otherwise("miss")).toMaybe(),
                                            equalTo(Maybe.of("error")));
        
            assertThat(just.matches(c->c.is(when("10"),then("hello")).is(when("2"),then("hello")),
                                    c->c.is(when(Predicates.instanceOf(Integer.class)), then("error")),
                                        otherwise("miss")).toMaybe(),
                                            equalTo(Maybe.of("error")));
            
            assertThat(just.matches(c->c.is(when("1"),then("hello"))
                                     .is(when("2"),then(()->"hello"))
                                     .is(when("3"),then(()->"hello")),
                                     c->c.is(when(Predicates.instanceOf(Integer.class)), then("error")),
                                     otherwise("miss")).toMaybe(),equalTo(Maybe.just("error")));
            
            
            assertThat(none.matches(c->c.is(when("1"),then("hello"))
                    .is(when("2"),then(()->"hello"))
                    .is(when("3"),then(()->"hello")),
                    c->c.is(when(Predicates.instanceOf(Integer.class)), then("error")),
                    otherwise("miss")).toMaybe(),equalTo(Maybe.just("miss")));
        
    }

	
	

	

	@Test
	public void testIterator1() {
		assertThat(StreamUtils.stream(just.iterator()).collect(Collectors.toList()),
				equalTo(Arrays.asList(10)));
	}

	@Test
	public void testForEach() {
		Mutable<Integer> capture = Mutable.of(null);
		 none.forEach(c->capture.set(c));
		assertNull(capture.get());
		just.forEach(c->capture.set(c));
		assertThat(capture.get(),equalTo(10));
	}

	@Test
	public void testSpliterator() {
		assertThat(StreamSupport.stream(just.spliterator(),false).collect(Collectors.toList()),
				equalTo(Arrays.asList(10)));
	}

	@Test
	public void testCast() {
		Xor<?,Number> num = just.cast(Number.class);
	}

	@Test
	public void testMapFunctionOfQsuperTQextendsR1() {
		assertThat(just.map(i->i+5),equalTo(Xor.primary(15)));
	}
	
	@Test
	public void testPeek() {
		Mutable<Integer> capture = Mutable.of(null);
		just = just.peek(c->capture.set(c));
		
		assertThat(capture.get(),equalTo(10));
	}

	private Trampoline<Integer> sum(int times,int sum){
		return times ==0 ?  Trampoline.done(sum) : Trampoline.more(()->sum(times-1,sum+times));
	}
	@Test
	public void testTrampoline() {
		assertThat(just.trampoline(n ->sum(10,n)),equalTo(Xor.primary(65)));
	}

	

	@Test
	public void testUnitT1() {
		assertThat(none.unit(10),equalTo(just));
	}

}
