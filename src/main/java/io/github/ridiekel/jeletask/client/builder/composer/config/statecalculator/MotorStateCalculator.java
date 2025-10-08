package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.utilities.Bytes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class MotorStateCalculator extends StateCalculatorSupport<MotorState> {
    private static final Mapper<ValidMotorDirectionState> DIRECTION_MAPPER = new Mapper<>(ValidMotorDirectionState.class, NumberConverter.UNSIGNED_BYTE)
            .add(ValidMotorDirectionState.UP, 1)
            .add(ValidMotorDirectionState.DOWN, 2)
            .add(ValidMotorDirectionState.STOP, 3)
            .add(ValidMotorDirectionState.START_STOP, 6)
            .add(ValidMotorDirectionState.UP_STOP, 7)
            .add(ValidMotorDirectionState.DOWN_STOP, 8)
            .add(ValidMotorDirectionState.UP_DOWN, 55)
            .add(ValidMotorDirectionState.MOTOR_GO_TO_POSITION, 11)
            .add(ValidMotorDirectionState.MOTOR_SUN_PROTECTION, 15);

    private static final OnOffToggleStateCalculator POWER_CALCULATOR = new OnOffToggleStateCalculator();
    private static final Mapper<ValidProtectionState> PROTECTION_STATE_MAPPER = new Mapper<>(ValidProtectionState.class, NumberConverter.UNSIGNED_BYTE)
            .add(ValidProtectionState.NOT_DEFINED, 0)
            .add(ValidProtectionState.ON_MOTOR_CONTROLLED_BY_PROTECTION, 1)
            .add(ValidProtectionState.ON_MOTOR_NOT_CONTROLLED_BY_PROTECTION, 2)
            .add(ValidProtectionState.ON_OVERRULED_BY_USER, 3)
            .add(ValidProtectionState.SWITCHED_OFF, 4);
    public static final BigDecimal HUNDRED = new BigDecimal(100);

    public MotorStateCalculator() {
    }

    @Override
    public MotorState fromEvent(ComponentSpec component, byte[] dataBytes) {
        MotorState state = new MotorState(DIRECTION_MAPPER.toEnum(dataBytes));
        if (dataBytes.length > 1) {
            state.setPower(POWER_CALCULATOR.fromEvent(component, new byte[]{dataBytes[1]}).getState());
        }
        if (dataBytes.length > 2) {
            state.setProtection(PROTECTION_STATE_MAPPER.toEnum(new byte[]{dataBytes[2]}));
        }
        if (dataBytes.length > 3) {
            state.setRequestedPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[3]}).intValue());
        }
        if (dataBytes.length > 4) {
            state.setCurrentPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[4]}).intValue());
        }
        if (dataBytes.length > 6) {
            state.setSecondsToFinish(BigDecimal.valueOf(NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[5], dataBytes[6]}).floatValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP));
        }
        if (dataBytes.length > 7) {
            state.setCorrectionAtZeroPercentInSeconds(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[7]}).intValue());
        }
        if (dataBytes.length > 8) {
            state.setCorrectionAtHundredPercentInSeconds(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[8]}).intValue());
        }
        return state;
    }

    @Override
    public byte[] toCommand(MotorState state) {
        byte[] setting = null;

        byte[] data = Bytes.EMPTY;

        if (state.getRequestedPosition() != null) {
            setting = DIRECTION_MAPPER.toBytes(ValidMotorDirectionState.MOTOR_GO_TO_POSITION);

            try {
                int position = (int) state.getRequestedPosition();
                position = Math.min(position, 100);
                position = Math.max(position, 0);
                data = NumberConverter.UNSIGNED_BYTE.convert(position);
            } catch (NumberFormatException e) {
                //Not a number, so no data is needed
            }
        }

        if (setting == null) {
            setting = DIRECTION_MAPPER.toBytes(state.getState());
        }

        return Bytes.concat(setting, data);
    }

    @Override
    public MotorState fromCommandForTesting(ComponentSpec component, byte[] dataBytes) {
        MotorState state = new MotorState(DIRECTION_MAPPER.toEnum(new byte[]{dataBytes[0]}));

        if (dataBytes.length > 1) {
            state.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
            state.setRequestedPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[1]}).intValue());
        }

        return state;
    }

    @Override
    public byte[] toEventForTesting(MotorState state) {
        byte[] power = POWER_CALCULATOR.toEventForTesting(new OnOffState(state.getPower()));
        byte[] direction = DIRECTION_MAPPER.toBytes(state.getState());
        byte[] protection = PROTECTION_STATE_MAPPER.toBytes(Optional.ofNullable(state.getProtection()).orElse(ValidProtectionState.NOT_DEFINED));
        byte[] currentPosition = Optional.ofNullable(state.getCurrentPosition()).map(p -> {
            p = Math.min(p, 100);
            p = Math.max(p, 0);
            return NumberConverter.UNSIGNED_BYTE.convert(p);
        }).orElse(Bytes.EMPTY);
        byte[] requestedPosition = Optional.ofNullable(state.getRequestedPosition()).map(p -> {
            p = Math.min(p, 100);
            p = Math.max(p, 0);
            return NumberConverter.UNSIGNED_BYTE.convert(p);
        }).orElse(currentPosition);
        byte[] secondsToFinish = Optional.ofNullable(state.getSecondsToFinish()).map(n -> n.multiply(HUNDRED)).map(NumberConverter.UNSIGNED_SHORT::convert).orElse(Bytes.EMPTY);
        byte[] correctionAtZeroPercentInSeconds = Optional.ofNullable(state.getCorrectionAtZeroPercentInSeconds()).map(NumberConverter.UNSIGNED_SHORT::convert).orElse(Bytes.EMPTY);
        byte[] correctionAtHundredPercentInSeconds = Optional.ofNullable(state.getCorrectionAtHundredPercentInSeconds()).map(NumberConverter.UNSIGNED_SHORT::convert).orElse(Bytes.EMPTY);

        return Bytes.concat(direction, power, protection, requestedPosition, currentPosition, secondsToFinish, correctionAtZeroPercentInSeconds, correctionAtHundredPercentInSeconds);
    }

    @Override
    public boolean isValidWriteState(MotorState state) {
        return state.getRequestedPosition() != null || super.isValidWriteState(state);
    }


    public enum ValidProtectionState {
        NOT_DEFINED, ON_MOTOR_CONTROLLED_BY_PROTECTION, ON_MOTOR_NOT_CONTROLLED_BY_PROTECTION, ON_OVERRULED_BY_USER, SWITCHED_OFF
    }

    public enum ValidMotorDirectionState {
        UP, DOWN, STOP, START_STOP, UP_STOP, DOWN_STOP, UP_DOWN, MOTOR_GO_TO_POSITION, MOTOR_SUN_PROTECTION
    }

    @Override
    protected Class<MotorState> getStateType() {
        return MotorState.class;
    }
}
