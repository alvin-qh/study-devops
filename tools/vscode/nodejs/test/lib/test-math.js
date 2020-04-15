const { expect } = require('chai');
const math = require('../../lib/math');

describe('Test math lib', () => {
    it('Should add function working', () => {
        expect(math.add(10, 20)).is.eql(30);
    });

    it('should sub function working', () => {
        expect(math.sub(10, 20)).is.eql(-10);
    });
});