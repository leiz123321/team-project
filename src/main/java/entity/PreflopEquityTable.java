package entity;

/**
 * The chances of winning and tying before the flop for each of the 169 kinds
 * of starting hand, against one opponent holding two random cards.
 *
 * <p>Before the flop the chances depend only on the two ranks and on whether
 * the cards share a suit, so 169 entries cover every starting hand. Counting
 * all of the roughly two billion outcomes for a hand takes too long to do
 * during a game, so the numbers were worked out ahead of time by
 * {@code PreflopEquityTableGenerator} in the test sources: 20 million random
 * deals per kind of hand, which makes each number accurate to about 0.0003.
 */
public class PreflopEquityTable {
    // Indexed by rank ordinal, TWO first. A pair is on the diagonal. A suited
    // hand is at [higher rank][lower rank]; an offsuit hand is at
    // [lower rank][higher rank].
    private static final double[][] WIN = {
        {0.4939, 0.2922, 0.3012, 0.3119, 0.3108, 0.3172, 0.3407, 0.3651, 0.3924, 0.4205, 0.4512, 0.4842, 0.5294},
        {0.3309, 0.5284, 0.3209, 0.3316, 0.3307, 0.3373, 0.3476, 0.3742, 0.4017, 0.4297, 0.4602, 0.4932, 0.5385},
        {0.3392, 0.3573, 0.5626, 0.3509, 0.3501, 0.3567, 0.3672, 0.3810, 0.4104, 0.4389, 0.4690, 0.5023, 0.5474},
        {0.3495, 0.3677, 0.3854, 0.5963, 0.3701, 0.3767, 0.3877, 0.4015, 0.4189, 0.4491, 0.4795, 0.5124, 0.5573},
        {0.3484, 0.3667, 0.3849, 0.4034, 0.6270, 0.3964, 0.4069, 0.4210, 0.4385, 0.4571, 0.4900, 0.5229, 0.5587},
        {0.3545, 0.3730, 0.3910, 0.4098, 0.4281, 0.6573, 0.4269, 0.4407, 0.4586, 0.4772, 0.4993, 0.5343, 0.5717},
        {0.3769, 0.3827, 0.4009, 0.4197, 0.4383, 0.4568, 0.6871, 0.4608, 0.4782, 0.4970, 0.5190, 0.5442, 0.5837},
        {0.3997, 0.4080, 0.4141, 0.4331, 0.4515, 0.4697, 0.4887, 0.7165, 0.4982, 0.5165, 0.5386, 0.5639, 0.5945},
        {0.4254, 0.4337, 0.4419, 0.4496, 0.4680, 0.4867, 0.5052, 0.5241, 0.7466, 0.5383, 0.5594, 0.5850, 0.6156},
        {0.4522, 0.4604, 0.4688, 0.4781, 0.4859, 0.5045, 0.5229, 0.5412, 0.5614, 0.7717, 0.5690, 0.5943, 0.6254},
        {0.4811, 0.4895, 0.4976, 0.5072, 0.5171, 0.5255, 0.5441, 0.5623, 0.5817, 0.5909, 0.7963, 0.6044, 0.6351},
        {0.5125, 0.5206, 0.5289, 0.5384, 0.5480, 0.5585, 0.5680, 0.5865, 0.6059, 0.6147, 0.6240, 0.8213, 0.6450},
        {0.5551, 0.5631, 0.5712, 0.5807, 0.5818, 0.5939, 0.6051, 0.6151, 0.6349, 0.6439, 0.6532, 0.6623, 0.8492},
    };
    private static final double[][] TIE = {
        {0.0190, 0.0612, 0.0615, 0.0618, 0.0599, 0.0575, 0.0549, 0.0516, 0.0486, 0.0460, 0.0437, 0.0418, 0.0396},
        {0.0579, 0.0171, 0.0615, 0.0620, 0.0601, 0.0577, 0.0548, 0.0519, 0.0488, 0.0462, 0.0439, 0.0420, 0.0398},
        {0.0582, 0.0582, 0.0153, 0.0616, 0.0601, 0.0578, 0.0548, 0.0518, 0.0490, 0.0464, 0.0441, 0.0420, 0.0400},
        {0.0584, 0.0587, 0.0585, 0.0137, 0.0586, 0.0568, 0.0538, 0.0507, 0.0478, 0.0456, 0.0433, 0.0412, 0.0392},
        {0.0566, 0.0569, 0.0571, 0.0557, 0.0117, 0.0534, 0.0509, 0.0477, 0.0449, 0.0426, 0.0406, 0.0385, 0.0362},
        {0.0543, 0.0547, 0.0548, 0.0539, 0.0508, 0.0102, 0.0471, 0.0445, 0.0416, 0.0391, 0.0373, 0.0355, 0.0334},
        {0.0518, 0.0519, 0.0519, 0.0512, 0.0485, 0.0450, 0.0089, 0.0406, 0.0380, 0.0355, 0.0334, 0.0318, 0.0299},
        {0.0488, 0.0492, 0.0491, 0.0482, 0.0455, 0.0425, 0.0388, 0.0078, 0.0343, 0.0322, 0.0299, 0.0280, 0.0264},
        {0.0460, 0.0463, 0.0466, 0.0455, 0.0428, 0.0398, 0.0365, 0.0329, 0.0070, 0.0284, 0.0269, 0.0249, 0.0231},
        {0.0434, 0.0438, 0.0440, 0.0433, 0.0406, 0.0375, 0.0341, 0.0310, 0.0275, 0.0063, 0.0246, 0.0225, 0.0206},
        {0.0413, 0.0416, 0.0418, 0.0411, 0.0386, 0.0356, 0.0320, 0.0288, 0.0260, 0.0237, 0.0059, 0.0204, 0.0184},
        {0.0394, 0.0397, 0.0399, 0.0392, 0.0368, 0.0339, 0.0304, 0.0270, 0.0240, 0.0218, 0.0198, 0.0056, 0.0170},
        {0.0375, 0.0378, 0.0379, 0.0370, 0.0345, 0.0320, 0.0288, 0.0255, 0.0223, 0.0199, 0.0179, 0.0165, 0.0055},
    };

    /**
     * Looks up the chances for two hole cards.
     *
     * @param first one hole card
     * @param second the other hole card
     * @return the chances of winning, tying, and losing before the flop
     * @throws IllegalArgumentException if a card is null or both are the same card
     */
    public EquityResult lookup(Card first, Card second) {
        if (first == null || second == null || first.equals(second)) {
            throw new IllegalArgumentException("need two different cards");
        }
        int high = Math.max(first.getRank().ordinal(), second.getRank().ordinal());
        int low = Math.min(first.getRank().ordinal(), second.getRank().ordinal());
        int row = low;
        int column = high;
        if (first.getSuit() == second.getSuit()) {
            row = high;
            column = low;
        }
        double win = WIN[row][column];
        double tie = TIE[row][column];
        return new EquityResult(win, tie, 1.0 - win - tie);
    }
}
