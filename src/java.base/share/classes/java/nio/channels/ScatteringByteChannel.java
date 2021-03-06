/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * 继承自 ReadableByteChannel
 * <p>
 * A channel that can read bytes into a sequence of buffers.
 *
 * <p> A <i>scattering</i> read operation reads, in a single invocation, a
 * sequence of bytes into one or more of a given sequence of buffers.
 * Scattering reads are often useful when implementing network protocols or
 * file formats that, for example, group data into segments consisting of one
 * or more fixed-length headers followed by a variable-length body.  Similar
 * <i>gathering</i> write operations are defined in the {@link
 * GatheringByteChannel} interface.
 * <p> 单次调用 scattering read 操作能够将 channel 中的数据读到一个或者多个 buffer 里去。
 * 所有的 buffer 中的数据组合起来构成从 channel 中读出来的所有的数据。
 * </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public interface ScatteringByteChannel
    extends ReadableByteChannel
{

    /**
     * Reads a sequence of bytes from this channel into a subsequence of the
     * given buffers.
     * <p> channel 中的数据读取到给定的 buffer 的子集中去。
     *
     * <p> An invocation of this method attempts to read up to <i>r</i> bytes
     * from this channel, where <i>r</i> is the total number of bytes remaining
     * the specified subsequence of the given buffer array, that is,
     * <p> 一次调用将尝试读取 r byte 的数据。r 的值为传入的 buffers 的 remain 空间的总和。
     * 如果 channel 中的数据有限，那么将只读取那一部分数据到部分的 buffers 中。
     *
     * <blockquote><pre>
     * dsts[offset].remaining()
     *     + dsts[offset+1].remaining()
     *     + ... + dsts[offset+length-1].remaining()</pre></blockquote>
     *
     * at the moment that this method is invoked.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is read, where
     * {@code 0}&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * Up to the first {@code dsts[offset].remaining()} bytes of this sequence
     * are transferred into buffer {@code dsts[offset]}, up to the next
     * {@code dsts[offset+1].remaining()} bytes are transferred into buffer
     * {@code dsts[offset+1]}, and so forth, until the entire byte sequence
     * is transferred into the given buffers.  As many bytes as possible are
     * transferred into each buffer, hence the final position of each updated
     * buffer, except the last updated buffer, is guaranteed to be equal to
     * that buffer's limit.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a read operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete.
     * <p> 如果存在一个线程已经在该 channel 上初始化了一个 read operation。那么当前线程再次调用
     * read operation 会被阻塞。
     * </p>
     *
     * @param  dsts
     *         The buffers into which bytes are to be transferred
     *
     * @param  offset
     *         The offset within the buffer array of the first buffer into
     *         which bytes are to be transferred; must be non-negative and no
     *         larger than {@code dsts.length}
     *
     * @param  length
     *         The maximum number of buffers to be accessed; must be
     *         non-negative and no larger than
     *         {@code dsts.length}&nbsp;-&nbsp;{@code offset}
     *
     * @return The number of bytes read, possibly zero,
     *         or {@code -1} if the channel has reached end-of-stream
     *
     * @throws  IndexOutOfBoundsException
     *          If the preconditions on the {@code offset} and {@code length}
     *          parameters do not hold
     *
     * @throws  NonReadableChannelException
     *          If this channel was not opened for reading
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the read operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the read operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException;

    /**
     * 与上面的方法类似，只不过 offset equals 0， length equals dsts.length
     * <p>
     * Reads a sequence of bytes from this channel into the given buffers.
     *
     * <p> An invocation of this method of the form {@code c.read(dsts)}
     * behaves in exactly the same manner as the invocation
     *
     * <blockquote><pre>
     * c.read(dsts, 0, dsts.length);</pre></blockquote>
     *
     * @param  dsts
     *         The buffers into which bytes are to be transferred
     *
     * @return The number of bytes read, possibly zero,
     *         or {@code -1} if the channel has reached end-of-stream
     *
     * @throws  NonReadableChannelException
     *          If this channel was not opened for reading
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the read operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the read operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public long read(ByteBuffer[] dsts) throws IOException;

}
