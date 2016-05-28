package old.ttorrent.client.strategy;

import java.util.BitSet;
import java.util.SortedSet;

import old.ttorrent.client.Piece;

/**
 * A sequential request strategy implementation.
 *
 * @author cjmalloy
 *
 */
public class RequestStrategyImplSequential implements RequestStrategy {

	@Override
	public Piece choosePiece(SortedSet<Piece> rarest, BitSet interesting,
			Piece[] pieces) {

		for (Piece p : pieces) {
			if (interesting.get(p.getIndex()))
				return p;
		}
		return null;
	}
}
