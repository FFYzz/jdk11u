/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;


/**
 * A multiplexor of {@link SelectableChannel} objects.
 * <p>
 *     SelectableChannel 对象的多路复用器
 *
 * <p> A selector may be created by invoking the {@link #open open} method of
 * this class, which will use the system's default {@link
 * java.nio.channels.spi.SelectorProvider selector provider} to
 * create a new selector.  A selector may also be created by invoking the
 * {@link java.nio.channels.spi.SelectorProvider#openSelector openSelector}
 * method of a custom selector provider.  A selector remains open until it is
 * closed via its {@link #close close} method.
 * <p>
 *     通过调用 open 方法会返回一个 selector 对象。会使用默认的 java.nio.channels.spi.
 *     SelectorProvider#openSelector 创建 selector。也可以通过调用 SelectorProvider#openSelector
 *     创建一个 selector。 调用 selector#close 方法之后，selector 才会关闭。
 *
 * <a id="ks"></a>
 *
 * <p> A selectable channel's registration with a selector is represented by a
 * {@link SelectionKey} object.  A selector maintains three sets of selection
 * keys:
 * <p>
 *     Selector 维护了以下三个 set
 *
 * <ul>
 *
 *   <li><p> The <i>key set</i> contains the keys representing the current
 *   channel registrations of this selector.  This set is returned by the
 *   {@link #keys() keys} method.
 *   <p>
 *       key set: 返回当前 selector 上注册的 SelectionKey
 *   </p></li>
 *
 *   <li><p> The <i>selected-key set</i> is the set of keys such that each
 *   key's channel was detected to be ready for at least one of the operations
 *   identified in the key's interest set during a prior selection operation
 *   that adds keys or updates keys in the set.
 *   This set is returned by the {@link #selectedKeys() selectedKeys} method.
 *   The selected-key set is always a subset of the key set.
 *   <p>
 *       selected-key set: 是 key set 的子集。这个集合中的 key 关联的 channel 的事件
 *       已经 ready，可以进行非阻塞操作。
 *   </p></li>
 *
 *   <li><p> The <i>cancelled-key</i> set is the set of keys that have been
 *   cancelled but whose channels have not yet been deregistered.  This set is
 *   not directly accessible.  The cancelled-key set is always a subset of the
 *   key set.
 *   <p>
 *       cancelled-key: key 已经取消，但是还没有反注册。该集合不能直接访问。
 *   </p></li>
 *
 * </ul>
 *
 * <p> All three sets are empty in a newly-created selector.
 * <p>
 *     刚创建的 selector 的三个 set 都为 empty
 *
 * <p> A key is added to a selector's key set as a side effect of registering a
 * channel via the channel's {@link SelectableChannel#register(Selector,int)
 * register} method.  Cancelled keys are removed from the key set during
 * selection operations.  The key set itself is not directly modifiable.
 *
 * <p>
 *     通过 SelectableChannel#register 注册 channel 的时候会顺带将 key 注册到 key set 中
 *     cancelled key 在 selection operation 的时候会被删除。
 *     key set 不被直接修改，而是通过其他操作，附带修改 key set。
 *
 * <p> A key is added to its selector's cancelled-key set when it is cancelled,
 * whether by closing its channel or by invoking its {@link SelectionKey#cancel
 * cancel} method.  Cancelling a key will cause its channel to be deregistered
 * during the next selection operation, at which time the key will be removed
 * from all of the selector's key sets.
 * <p>
 *     调用 SelectionKey#cancel 方法或者关闭 channel 导致 key 被取消的时候，
 *     key 会被加入到 cancelled-key set 中。被取消的 key 所绑定的 channel 会在进行下一次
 *     selection operation 的时候 被反注册，key 也会从 key set 中移除。
 *
 * <a id="sks"></a><p> Keys are added to the selected-key set by selection
 * operations.  A key may be removed directly from the selected-key set by
 * invoking the set's {@link java.util.Set#remove(java.lang.Object) remove}
 * method or by invoking the {@link java.util.Iterator#remove() remove} method
 * of an {@link java.util.Iterator iterator} obtained from the set.
 * All keys may be removed from the selected-key set by invoking the set's
 * {@link java.util.Set#clear() clear} method.  Keys may not be added directly
 * to the selected-key set. </p>
 * <p>
 *     通过 selection operation，key 会被加入到 selected-key set 中。通过调用 Set#remove
 *     方法，可以直接将 key 从 selected-key set 中移除。或者通过调用 Iterator#remove 方法
 *     进行移除。key 不能够直接加入到 selected-key set 中。可以通过 Set#clear 方法清除 selected-key set
 *     中的所有元素
 *
 * <a id="selop"></a>
 * <h2>Selection</h2>
 *
 * <p> A selection operation queries the underlying operating system for an
 * update as to the readiness of each registered channel to perform any of the
 * operations identified by its key's interest set.  There are two forms of
 * selection operation:
 * <p>
 *     selection operation 会去查询底层操作系统，检查每个注册的 channel 的可读性的更新。
 *     以决定继续执行 key 所感兴趣的事件。有以下两种 selection operation:
 *
 * <ol>
 *
 *   <li><p> The {@link #select()}, {@link #select(long)}, and {@link #selectNow()}
 *   methods add the keys of channels ready to perform an operation to the
 *   selected-key set, or update the ready-operation set of keys already in the
 *   selected-key set.
 *   <p>
 *       select()/select(long)/selectNow() 方法: 将 key 添加到 selected-key set 中，
 *       或者更新 selected-key set 中的 key。
 *   </p></li>
 *
 *   <li><p> The {@link #select(Consumer)}, {@link #select(Consumer, long)}, and
 *   {@link #selectNow(Consumer)} methods perform an <i>action</i> on the key
 *   of each channel that is ready to perform an operation.  These methods do
 *   not add to the selected-key set.
 *   <p>
 *       select(Consumer)/select(Consumer, long)/selectNow(Consumer) 方法: 当
 *       channel 已经 ready 之后，会去执行一个 action。以上方法不会将 key 添加到
 *       selected-key set 中。
 *   </p></li>
 *
 * </ol>
 *
 * <h3>Selection operations that add to the selected-key set</h3>
 *
 * <p> During each selection operation, keys may be added to and removed from a
 * selector's selected-key set and may be removed from its key and
 * cancelled-key sets.  Selection is performed by the {@link #select()}, {@link
 * #select(long)}, and {@link #selectNow()} methods, and involves three steps:
 * </p>
 *
 * <p>
 *     在执行 selection operation 的时候，key 可能被加入到 selected-key set 中，也可能被移出
 *     selected-key set。也可能从 cancelled set 中移除 key。selection 的调用有以下三步:
 * </p>
 *
 * <ol>
 *
 *   <li><p> Each key in the cancelled-key set is removed from each key set of
 *   which it is a member, and its channel is deregistered.  This step leaves
 *   the cancelled-key set empty.
 *   <p>
 *       在 cancelled-key set 中的 key 将被移除，并且相关的 channel 会被反注册。
 *       该操作之后 cancelled-key set 中将不存在元素。
 *   </p></li>
 *
 *   <li><p> The underlying operating system is queried for an update as to the
 *   readiness of each remaining channel to perform any of the operations
 *   identified by its key's interest set as of the moment that the selection
 *   operation began.  For a channel that is ready for at least one such
 *   operation, one of the following two actions is performed:
 *   <p>
 *       底层操作系统会去查询是否有已经可以操作的 channel。
 *   </p>
 *   <ol>
 *
 *     <li><p> If the channel's key is not already in the selected-key set then
 *     it is added to that set and its ready-operation set is modified to
 *     identify exactly those operations for which the channel is now reported
 *     to be ready.  Any readiness information previously recorded in the ready
 *     set is discarded.
 *     <p>
 *         如果 channel 关联的 key 之前不在 selected-key set 中，那么会被加入到 selected-key
 *         set 中。ready-operation set 中会加入已经被确认为 ready 的 key。并且之前在 ready-operation set
 *         中的可读的 key 将会被丢弃。
 *     </p></li>
 *
 *     <li><p> Otherwise the channel's key is already in the selected-key set,
 *     so its ready-operation set is modified to identify any new operations
 *     for which the channel is reported to be ready.  Any readiness
 *     information previously recorded in the ready set is preserved; in other
 *     words, the ready set returned by the underlying system is
 *     bitwise-disjoined into the key's current ready set. </p></li>
 *
 *   </ol>
 *
 *   If all of the keys in the key set at the start of this step have empty
 *   interest sets then neither the selected-key set nor any of the keys'
 *   ready-operation sets will be updated.
 *
 *   <li><p> If any keys were added to the cancelled-key set while step (2) was
 *   in progress then they are processed as in step (1). </p></li>
 *
 * </ol>
 *
 * <p> Whether or not a selection operation blocks to wait for one or more
 * channels to become ready, and if so for how long, is the only essential
 * difference between the three selection methods. </p>
 *
 * <p>
 *     if so for how long: 如果是这样，那又会持续多久
 *     无论 selection operation 是阻塞在等待一个或者多个 channel 变成 ready，
 *     等待过程需要多久是三种 selection 方法的唯一重要的区别。
 * </p>
 *
 * <h3>Selection operations that perform an action on selected keys</h3>
 *
 * <p> During each selection operation, keys may be removed from the selector's
 * key, selected-key, and cancelled-key sets.  Selection is performed by the
 * {@link #select(Consumer)}, {@link #select(Consumer,long)}, and {@link
 * #selectNow(Consumer)} methods, and involves three steps:  </p>
 *
 * <ol>
 *
 *   <li><p> Each key in the cancelled-key set is removed from each key set of
 *   which it is a member, and its channel is deregistered.  This step leaves
 *   the cancelled-key set empty. </p></li>
 *
 *   <li><p> The underlying operating system is queried for an update as to the
 *   readiness of each remaining channel to perform any of the operations
 *   identified by its key's interest set as of the moment that the selection
 *   operation began.
 *
 *   <p> For a channel that is ready for at least one such operation, the
 *   ready-operation set of the channel's key is set to identify exactly the
 *   operations for which the channel is ready and the <i>action</i> specified
 *   to the {@code select} method is invoked to consume the channel's key.  Any
 *   readiness information previously recorded in the ready set is discarded
 *   prior to invoking the <i>action</i>.
 *
 *   <p> Alternatively, where a channel is ready for more than one operation,
 *   the <i>action</i> may be invoked more than once with the channel's key and
 *   ready-operation set modified to a subset of the operations for which the
 *   channel is ready.  Where the <i>action</i> is invoked more than once for
 *   the same key then its ready-operation set never contains operation bits
 *   that were contained in the set at previous calls to the <i>action</i>
 *   in the same selection operation.  </p></li>
 *
 *   <li><p> If any keys were added to the cancelled-key set while step (2) was
 *   in progress then they are processed as in step (1). </p></li>
 *
 * </ol>
 *
 *
 * <h2>Concurrency</h2>
 *
 * <p> A Selector and its key set are safe for use by multiple concurrent
 * threads.  Its selected-key set and cancelled-key set, however, are not.
 * <p>
 *     Selector 和 key set 是线程安全的，但是 selected-key set 和 cancelled-key set
 *     不是线程安全的
 *
 * <p> The selection operations synchronize on the selector itself, on the
 * selected-key set, in that order.  They also synchronize on the cancelled-key
 * set during steps (1) and (3) above.
 *
 * <p> Changes made to the interest sets of a selector's keys while a
 * selection operation is in progress have no effect upon that operation; they
 * will be seen by the next selection operation.
 *
 * <p> Keys may be cancelled and channels may be closed at any time.  Hence the
 * presence of a key in one or more of a selector's key sets does not imply
 * that the key is valid or that its channel is open. Application code should
 * be careful to synchronize and check these conditions as necessary if there
 * is any possibility that another thread will cancel a key or close a channel.
 * <p>
 *     key 可能随时会被取消，channel 也可能随时关闭。因此，key 出现在 selector 的任意 key
 *     set 中并不代表 key 是有效的或者 channal 是开启的。
 *     多线程环境下要注意这种情况。
 *
 * <p> A thread blocked in a selection operation may be interrupted by some
 * other thread in one of three ways:
 * <p>
 *     一个线程阻塞在 selection operation 中可能被以下三种情况中断:
 *
 * <ul>
 *
 *   <li><p> By invoking the selector's {@link #wakeup wakeup} method,
 *   <p>
 *       调用了 selector 的 wakeup 方法
 *   </p></li>
 *
 *   <li><p> By invoking the selector's {@link #close close} method, or
 *   <p>
 *       调用了 selector 的 close 方法
 *   </p></li>
 *
 *   <li><p> By invoking the blocked thread's {@link
 *   java.lang.Thread#interrupt() interrupt} method, in which case its
 *   interrupt status will be set and the selector's {@link #wakeup wakeup}
 *   method will be invoked.
 *   <p>
 *       调用了阻塞线程的 interrupt 方法。当前线程的中断状态会被设置， selector 的 wakeup
 *       方法会被调用。
 *   </p></li>
 *
 * </ul>
 *
 * <p> The {@link #close close} method synchronizes on the selector and its
 * selected-key set in the same order as in a selection operation.
 *
 * <a id="ksc"></a>
 * <p> A Selector's key set is safe for use by multiple concurrent threads.
 * Retrieval operations from the key set do not generally block and so may
 * overlap with new registrations that add to the set, or with the cancellation
 * steps of selection operations that remove keys from the set.  Iterators and
 * spliterators return elements reflecting the state of the set at some point at
 * or since the creation of the iterator/spliterator.  They do not throw
 * {@link java.util.ConcurrentModificationException ConcurrentModificationException}.
 * <p>
 *     selector 的 key set 是线程安全的。key set 的获取操作一般不被 block，所以获取操作可能与 add
 *     /cancel 操作出现并发的情况。Iterators 和 spliterators (两者都是迭代器) 会返回一些 state 来表示，
 *     并不会抛出 ConcurrentModificationException 异常。
 *
 * <a id="sksc"></a>
 * <p> A selector's selected-key set is not, in general, safe for use by
 * multiple concurrent threads.  If such a thread might modify the set directly
 * then access should be controlled by synchronizing on the set itself.  The
 * iterators returned by the set's {@link java.util.Set#iterator() iterator}
 * methods are <i>fail-fast:</i> If the set is modified after the iterator is
 * created, in any way except by invoking the iterator's own {@link
 * java.util.Iterator#remove() remove} method, then a {@link
 * java.util.ConcurrentModificationException} will be thrown. </p>
 * <p>
 *     selector 的 selected-key 不是线程安全的。如果一个线程要修改 selected-key set，
 *     那么需要加上同步器。迭代器支持 fail-fast，会抛出 ConcurrentModificationException 异常。
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 *
 * @see SelectableChannel
 * @see SelectionKey
 */

public abstract class Selector implements Closeable {

    /**
     * 初始化一个 Selector 实例
     * <p>
     * Initializes a new instance of this class.
     */
    protected Selector() { }

    /**
     * 通过调用 SelectorProvider#openSelector 方法开启一个 Selector
     * <p>
     * Opens a selector.
     *
     * <p> The new selector is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openSelector openSelector} method
     * of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.  </p>
     *
     * @return  A new selector
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
     * 返回当前 Selector 是否开启
     * <p>
     * Tells whether or not this selector is open.
     *
     * @return {@code true} if, and only if, this selector is open
     */
    public abstract boolean isOpen();

    /**
     * 返回创建该 Selector 的 SelectorProvider
     * <p>
     * Returns the provider that created this channel.
     *
     * @return  The provider that created this channel
     */
    public abstract SelectorProvider provider();

    /**
     * Selector 的 key set
     * <p>
     * Returns this selector's key set.
     *
     * <p>
     *     key set 不被直接修改，从 key set 中移除 key 当且仅当 key 被 cancel 且 channel 被反注册的
     *     情况下才会发生。任意尝试修改 key set 的操作都会抛出 UnsupportedOperationException 异常
     * <p> The key set is not directly modifiable.  A key is removed only after
     * it has been cancelled and its channel has been deregistered.  Any
     * attempt to modify the key set will cause an {@link
     * UnsupportedOperationException} to be thrown.
     *
     * <p> The set is <a href="#ksc">safe</a> for use by multiple concurrent
     * threads.  </p>
     *
     * @return  This selector's key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> keys();

    /**
     * 返回 Selector 的 selected-key set
     * <p>
     * Returns this selector's selected-key set.
     * <p>
     *     key 可能被直接从 selected-key set 中移除，但是不能够直接加入大
     *     selected-key set 中。任何尝试直接加入到 selected-key set 的操作
     *     都会抛出 UnsupportedOperationException 异常。
     * <p> Keys may be removed from, but not directly added to, the
     * selected-key set.  Any attempt to add an object to the key set will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * <p> The selected-key set is <a href="#sksc">not thread-safe</a>.  </p>
     *
     * @return  This selector's selected-key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> selectedKeys();

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     * <p>
     *     selection operation。select 那些已经 ready 的 channel。
     *
     * <p> This method performs a non-blocking <a href="#selop">selection
     * operation</a>.  If no channels have become selectable since the previous
     * selection operation then this method immediately returns zero.
     * <p>
     *     该方法时非阻塞方法，如果没有 channel 是 ready 的，直接返回 0
     *
     * <p> Invoking this method clears the effect of any previous invocations
     * of the {@link #wakeup wakeup} method.  </p>
     * <p>
     *     调用该方法可以清理之前调用 wakeup 方法造成的影响。
     *     比如清理 cancel 的 key。
     *
     * @return  The number of keys, possibly zero, whose ready-operation sets
     *          were updated by the selection operation
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract int selectNow() throws IOException;

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     * <p>
     *     selection operation。select 那些已经 ready 的 channel。
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It returns only after at least one channel is selected,
     * this selector's {@link #wakeup wakeup} method is invoked, the current
     * thread is interrupted, or the given timeout period expires, whichever
     * comes first.
     * <p>
     *     阻塞的 selection operation 操作。该方法的返回前提是
     *     1. 至少有一个 channel 已经 ready。
     *     2. 调用了 Selector 的 wakeup 方法。
     *     3. 阻塞线程被中断。
     *     4. 指定时间超时。
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method. </p>
     * <p>
     *     该方法不提供实时的保障。
     *
     * @param  timeout  If positive, block for up to {@code timeout}
     *                  milliseconds, more or less, while waiting for a
     *                  channel to become ready; if zero, block indefinitely;
     *                  must not be negative
     *
     * @return  The number of keys, possibly zero,
     *          whose ready-operation sets were updated
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     */
    public abstract int select(long timeout) throws IOException;

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It returns only after at least one channel is selected,
     * this selector's {@link #wakeup wakeup} method is invoked, or the current
     * thread is interrupted, whichever comes first.  </p>
     * <p>
     *     block 的 selection operation。有三种情况会在阻塞中被唤醒：
     *     1. 至少有一个 channal 已经 ready。
     *     2. 调用了 selector 的 wakeup 方法。
     *     3. 当前线程被中断。
     *
     * @return  The number of keys, possibly zero,
     *          whose ready-operation sets were updated
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract int select() throws IOException;

    /**
     * Selects and performs an action on the keys whose corresponding channels
     * are ready for I/O operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It wakes up from querying the operating system only when
     * at least one channel is selected, this selector's {@link #wakeup wakeup}
     * method is invoked, the current thread is interrupted, or the given
     * timeout period expires, whichever comes first.
     * <p>
     *     blocking selection operation。从阻塞中醒过来有以下几种情况:
     *     1. 至少有一个 channel 已经 ready。
     *     2. 调用了 selector 的 wakeup 方法。
     *     3. 当前线程被中断。
     *     4. 指定时间超时。
     *
     * <p> The specified <i>action</i>'s {@link Consumer#accept(Object) accept}
     * method is invoked with the key for each channel that is ready to perform
     * an operation identified by its key's interest set.  The {@code accept}
     * method may be invoked more than once for the same key but with the
     * ready-operation set containing a subset of the operations for which the
     * channel is ready (as described above).  The {@code accept} method is
     * invoked while synchronized on the selector and its selected-key set.
     * Great care must be taken to avoid deadlocking with other threads that
     * also synchronize on these objects.  Selection operations are not reentrant
     * in general and consequently the <i>action</i> should take great care not
     * to attempt a selection operation on the same selector.  The behavior when
     * attempting a reentrant selection operation is implementation specific and
     * therefore not specified.  If the <i>action</i> closes the selector then
     * {@code ClosedSelectorException} is thrown when the action completes.
     * The <i>action</i> is not prohibited from closing channels registered with
     * the selector, nor prohibited from cancelling keys or changing a key's
     * interest set.  If a channel is selected but its key is cancelled or its
     * interest set changed before the <i>action</i> is performed on the key
     * then it is implementation specific as to whether the <i>action</i> is
     * invoked (it may be invoked with an {@link SelectionKey#isValid() invalid}
     * key).  Exceptions thrown by the action are relayed to the caller.
     * <p>
     *     当 channel ready 的时候，会调用 Consumer 的 action。同一个 key 的 accept 方法
     *     可能会调用多次，
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method.
     *
     * @implSpec The default implementation removes all keys from the
     * selected-key set, invokes {@link #select(long) select(long)} with the
     * given timeout and then performs the action for each key added to the
     * selected-key set.  The default implementation does not detect the action
     * performing a reentrant selection operation.  The selected-key set may
     * or may not be empty on completion of the default implementation.
     *
     * @param  action   The action to perform
     *
     * @param  timeout  If positive, block for up to {@code timeout}
     *                  milliseconds, more or less, while waiting for a
     *                  channel to become ready; if zero, block indefinitely;
     *                  must not be negative
     *
     * @return  The number of unique keys consumed, possibly zero
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed or is closed by the action
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     *
     * @since 11
     */
    public int select(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout");
        return doSelect(Objects.requireNonNull(action), timeout);
    }

    /**
     * Selects and performs an action on the keys whose corresponding channels
     * are ready for I/O operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It wakes up from querying the operating system only when
     * at least one channel is selected, this selector's {@link #wakeup wakeup}
     * method is invoked, or the current thread is interrupted, whichever comes
     * first.
     *
     * <p> This method is equivalent to invoking the 2-arg
     * {@link #select(Consumer, long) select} method with a timeout of {@code 0}
     * to block indefinitely.  </p>
     *
     * @implSpec The default implementation invokes the 2-arg {@code select}
     * method with a timeout of {@code 0}.
     *
     * @param  action   The action to perform
     *
     * @return  The number of unique keys consumed, possibly zero
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed or is closed by the action
     *
     * @since 11
     */
    public int select(Consumer<SelectionKey> action) throws IOException {
        return select(action, 0);
    }

    /**
     * Selects and performs an action on the keys whose corresponding channels
     * are ready for I/O operations.
     *
     * <p> This method performs a non-blocking <a href="#selop">selection
     * operation</a>.
     *
     * <p> Invoking this method clears the effect of any previous invocations
     * of the {@link #wakeup wakeup} method.  </p>
     *
     * @implSpec The default implementation removes all keys from the
     * selected-key set, invokes {@link #selectNow() selectNow()} and then
     * performs the action for each key added to the selected-key set.  The
     * default implementation does not detect the action performing a reentrant
     * selection operation.  The selected-key set may or may not be empty on
     * completion of the default implementation.
     * <p>
     *     进入 selected-key set 的 key 会执行 accept 方法
     *
     * @param  action   The action to perform
     *
     * @return  The number of unique keys consumed, possibly zero
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed or is closed by the action
     *
     * @since 11
     */
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        return doSelect(Objects.requireNonNull(action), -1);
    }

    /**
     * Default implementation of select(Consumer) and selectNow(Consumer).
     */
    private int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        // 加锁锁住
        synchronized (this) {
            // 获取当前的 selected keys
            Set<SelectionKey> selectedKeys = selectedKeys();
            // 加锁
            synchronized (selectedKeys) {
                // 全部清理掉
                selectedKeys.clear();
                int numKeySelected;
                // 没有设置超时等待
                if (timeout < 0) {
                    // 直接调用 selectNow，马上返回
                    numKeySelected = selectNow();
                } else {
                    // 带超时的 select
                    numKeySelected = select(timeout);
                }

                // copy selected-key set as action may remove keys
                Set<SelectionKey> keysToConsume = Set.copyOf(selectedKeys);
                // 检查是不是有并发修改的情况
                assert keysToConsume.size() == numKeySelected;
                selectedKeys.clear();

                // invoke action for each selected key
                keysToConsume.forEach(k -> {
                    // 执行 accept 方法
                    action.accept(k);
                    if (!isOpen())
                        throw new ClosedSelectorException();
                });

                // 返回 ready 的个数
                return numKeySelected;
            }
        }
    }


    /**
     * Causes the first selection operation that has not yet returned to return
     * immediately.
     * <p>
     *     唤醒第一个阻塞在 select 上的线程
     *
     * <p> If another thread is currently blocked in a selection operation then
     * that invocation will return immediately.  If no selection operation is
     * currently in progress then the next invocation of a selection operation
     * will return immediately unless {@link #selectNow()} or {@link
     * #selectNow(Consumer)} is invoked in the meantime.  In any case the value
     * returned by that invocation may be non-zero.  Subsequent selection
     * operations will block as usual unless this method is invoked again in the
     * meantime.
     * <p>
     *     若当前有阻塞的 select 线程，那么直接唤醒第一个阻塞在 select 上的线程。
     *     如果当前没有阻塞在 select 上的线程，那么下一个调用 select 的线程，即使没有 ready，
     *     也会立即返回。
     *
     * <p> Invoking this method more than once between two successive selection
     * operations has the same effect as invoking it just once.  </p>
     * <p>
     *     没有阻塞在 select 的线程的情况下，多次调用 wakeup相当于只调用了一次 wakeup。
     *     类似于 LockSupport 的 unpark 方法。
     *
     * @return  This selector
     */
    public abstract Selector wakeup();

    /**
     * 关闭当前的 Selector
     * <p>
     * Closes this selector.
     *
     * <p> If a thread is currently blocked in one of this selector's selection
     * methods then it is interrupted as if by invoking the selector's {@link
     * #wakeup wakeup} method.
     * <p>
     *     如果线程当前阻塞在 selector 的 select 方法上，那么 selector 会被中断。与调用
     *     wakeup 方法造成的影响是一致的。
     *
     * <p> Any uncancelled keys still associated with this selector are
     * invalidated, their channels are deregistered, and any other resources
     * associated with this selector are released.
     * <p>
     *     所有在该 selector 上的，没有被 cancel 的 key 都会失效，key 关联的 channel 也会被
     *     反注册，所有与之关联的资源也会被释放。
     *
     * <p> If this selector is already closed then invoking this method has no
     * effect.
     * <p>
     *     多次调用该方法没有任何效果。
     *
     * <p> After a selector is closed, any further attempt to use it, except by
     * invoking this method or the {@link #wakeup wakeup} method, will cause a
     * {@link ClosedSelectorException} to be thrown. </p>
     * <p>
     *     当 selector close 之后，除了调用 wakeup 和 close 方法之外的其他方法都会抛出
     *     ClosedSelectorException 异常。
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void close() throws IOException;
}
